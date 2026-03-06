package com.goldenraspberry.awards.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goldenraspberry.awards.dto.AwardsIntervalResponseDTO;
import com.goldenraspberry.awards.dto.ProducerIntervalDTO;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.goldenraspberry.awards.AwardsApplication.class)
@AutoConfigureWebMvc
class AwardsControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testGetProducerIntervals() throws Exception {
        // Calcular resultados esperados baseados no arquivo CSV padrão
        AwardsIntervalResponseDTO expectedResponse = calculateExpectedIntervalsFromCsv();

        // Chamar a API
        String response = mockMvc.perform(get("/api/awards/intervals")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.min").isArray())
                .andExpect(jsonPath("$.max").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AwardsIntervalResponseDTO actualResponse = objectMapper.readValue(response, AwardsIntervalResponseDTO.class);

        // Verificar que a resposta não é nula
        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMin());
        assertNotNull(actualResponse.getMax());

        // Comparar resultados exatos com os esperados do arquivo CSV padrão
        compareIntervals("min", expectedResponse.getMin(), actualResponse.getMin());
        compareIntervals("max", expectedResponse.getMax(), actualResponse.getMax());
    }

    /**
     * Calcula os intervalos esperados diretamente do arquivo CSV padrão.
     * Este método garante que qualquer modificação no arquivo CSV será detectada.
     */
    private AwardsIntervalResponseDTO calculateExpectedIntervalsFromCsv() throws Exception {
        ClassPathResource resource = new ClassPathResource("movielist.csv");
        Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        
        CSVFormat format = CSVFormat.Builder.create()
                .setDelimiter(';')
                .setHeader()
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        CSVParser csvParser = new CSVParser(reader, format);

        try {
            // Agrupar anos de vitórias por produtor em uma única passada usando streams
            Map<String, List<Integer>> producerWins = csvParser.stream()
                    .filter(record -> "yes".equalsIgnoreCase(record.get("winner").trim()))
                    .flatMap(record -> {
                        Integer year = Integer.parseInt(record.get("year").trim());
                        String producersStr = record.get("producers").trim();
                        Set<String> producerNames = parseProducerNames(producersStr);
                        return producerNames.stream()
                                .map(producerName -> new AbstractMap.SimpleEntry<>(producerName, year));
                    })
                    .collect(Collectors.groupingBy(
                            Map.Entry::getKey,
                            Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                    ));

            // Calcular todos os intervalos usando streams
            List<ProducerIntervalDTO> allIntervals = producerWins.entrySet().stream()
                    .flatMap(entry -> calculateIntervals(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            if (allIntervals.isEmpty()) {
                return new AwardsIntervalResponseDTO(new ArrayList<>(), new ArrayList<>());
            }

            // Encontrar min, max e filtrar em uma única passada otimizada
            List<ProducerIntervalDTO> minIntervals = new ArrayList<>();
            List<ProducerIntervalDTO> maxIntervals = new ArrayList<>();
            int minInterval = Integer.MAX_VALUE;
            int maxInterval = Integer.MIN_VALUE;

            for (ProducerIntervalDTO dto : allIntervals) {
                int interval = dto.getInterval();
                if (interval < minInterval) {
                    minInterval = interval;
                    minIntervals.clear();
                    minIntervals.add(dto);
                } else if (interval == minInterval) {
                    minIntervals.add(dto);
                }

                if (interval > maxInterval) {
                    maxInterval = interval;
                    maxIntervals.clear();
                    maxIntervals.add(dto);
                } else if (interval == maxInterval) {
                    maxIntervals.add(dto);
                }
            }

            // Ordenar para comparação consistente
            sortIntervals(minIntervals);
            sortIntervals(maxIntervals);

            return new AwardsIntervalResponseDTO(minIntervals, maxIntervals);
        } finally {
            csvParser.close();
            reader.close();
        }
    }

    /**
     * Parse dos nomes de produtores, tratando vírgulas e "and"
     * Otimizado usando streams
     */
    private Set<String> parseProducerNames(String producersStr) {
        if (producersStr == null || producersStr.trim().isEmpty()) {
            return new HashSet<>();
        }

        // Remove "and" e substitui por vírgula, depois divide por vírgula usando streams
        String normalized = producersStr.replaceAll("\\s+and\\s+", ", ");
        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Calcula os intervalos entre anos consecutivos para um produtor.
     * Os anos são ordenados e os intervalos são calculados em uma única passada.
     */
    private Stream<ProducerIntervalDTO> calculateIntervals(String producerName, List<Integer> years) {
        if (years.size() < 2) {
            return Stream.empty();
        }

        List<Integer> sortedYears = years.stream()
                .sorted()
                .collect(Collectors.toList());

        return IntStream.range(0, sortedYears.size() - 1)
                .mapToObj(i -> {
                    int previousWin = sortedYears.get(i);
                    int followingWin = sortedYears.get(i + 1);
                    ProducerIntervalDTO dto = new ProducerIntervalDTO();
                    dto.setProducer(producerName);
                    dto.setInterval(followingWin - previousWin);
                    dto.setPreviousWin(previousWin);
                    dto.setFollowingWin(followingWin);
                    return dto;
                });
    }

    /**
     * Ordena os intervalos para comparação consistente
     */
    private void sortIntervals(List<ProducerIntervalDTO> intervals) {
        intervals.sort((a, b) -> {
            int compare = a.getProducer().compareTo(b.getProducer());
            if (compare != 0) return compare;
            compare = a.getPreviousWin().compareTo(b.getPreviousWin());
            if (compare != 0) return compare;
            return a.getFollowingWin().compareTo(b.getFollowingWin());
        });
    }

    /**
     * Compara os intervalos retornados pela API com os esperados do CSV.
     * Falha se houver qualquer diferença.
     */
    private void compareIntervals(String type, List<ProducerIntervalDTO> expected, List<ProducerIntervalDTO> actual) {
        // Ordenar os intervalos atuais para comparação
        sortIntervals(actual);

        assertEquals(expected.size(), actual.size(), 
                String.format("O número de intervalos %s esperados (%d) não corresponde ao retornado (%d). " +
                        "O arquivo CSV pode ter sido modificado.", type, expected.size(), actual.size()));

        for (int i = 0; i < expected.size(); i++) {
            ProducerIntervalDTO expectedDto = expected.get(i);
            ProducerIntervalDTO actualDto = actual.get(i);

            assertEquals(expectedDto.getProducer(), actualDto.getProducer(),
                    String.format("Produtor no intervalo %s [%d] não corresponde. Esperado: %s, Atual: %s. " +
                            "O arquivo CSV pode ter sido modificado.", type, i, expectedDto.getProducer(), actualDto.getProducer()));

            assertEquals(expectedDto.getInterval(), actualDto.getInterval(),
                    String.format("Intervalo %s [%d] não corresponde para o produtor %s. Esperado: %d, Atual: %d. " +
                            "O arquivo CSV pode ter sido modificado.", type, i, expectedDto.getProducer(), 
                            expectedDto.getInterval(), actualDto.getInterval()));

            assertEquals(expectedDto.getPreviousWin(), actualDto.getPreviousWin(),
                    String.format("Ano anterior no intervalo %s [%d] não corresponde para o produtor %s. " +
                            "Esperado: %d, Atual: %d. O arquivo CSV pode ter sido modificado.", type, i, 
                            expectedDto.getProducer(), expectedDto.getPreviousWin(), actualDto.getPreviousWin()));

            assertEquals(expectedDto.getFollowingWin(), actualDto.getFollowingWin(),
                    String.format("Ano seguinte no intervalo %s [%d] não corresponde para o produtor %s. " +
                            "Esperado: %d, Atual: %d. O arquivo CSV pode ter sido modificado.", type, i, 
                            expectedDto.getProducer(), expectedDto.getFollowingWin(), actualDto.getFollowingWin()));
        }
    }
}

