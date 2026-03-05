package com.goldenraspberry.awards.service;

import com.goldenraspberry.awards.model.Movie;
import com.goldenraspberry.awards.model.Producer;
import com.goldenraspberry.awards.repository.MovieRepository;
import com.goldenraspberry.awards.repository.ProducerRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Service
public class CsvDataLoaderService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ProducerRepository producerRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadCsvData() {
        try {
            ClassPathResource resource = new ClassPathResource("movielist.csv");
            Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            
            CSVFormat format = CSVFormat.Builder.create()
                    .setDelimiter(';')
                    .setHeader()
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser csvParser = new CSVParser(reader, format);

            for (CSVRecord record : csvParser) {
                Integer year = Integer.parseInt(record.get("year").trim());
                String title = record.get("title").trim();
                String studios = record.get("studios").trim();
                String producersStr = record.get("producers").trim();
                String winnerStr = record.get("winner").trim();
                
                Boolean winner = "yes".equalsIgnoreCase(winnerStr);

                Movie movie = new Movie();
                movie.setYear(year);
                movie.setTitle(title);
                movie.setStudios(studios);
                movie.setWinner(winner);
                movie.setProducers(new HashSet<>());

                // Parse producers (pode ter múltiplos separados por vírgula e "and")
                Set<Producer> producers = parseProducers(producersStr);
                movie.setProducers(producers);

                movieRepository.save(movie);
            }

            csvParser.close();
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar dados do CSV", e);
        }
    }

    private Set<Producer> parseProducers(String producersStr) {
        Set<Producer> producers = new HashSet<>();
        
        if (producersStr == null || producersStr.trim().isEmpty()) {
            return producers;
        }

        // Remove "and" e substitui por vírgula, depois divide por vírgula
        String normalized = producersStr.replaceAll("\\s+and\\s+", ", ");
        String[] producerNames = normalized.split(",");

        for (String producerName : producerNames) {
            final String trimmedProducerName = producerName.trim();
            if (!trimmedProducerName.isEmpty()) {
                Producer producer = producerRepository.findByName(trimmedProducerName)
                        .orElseGet(() -> {
                            Producer newProducer = new Producer();
                            newProducer.setName(trimmedProducerName);
                            return producerRepository.save(newProducer);
                        });
                producers.add(producer);
            }
        }

        return producers;
    }
}

