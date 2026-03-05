package com.goldenraspberry.awards.service;

import com.goldenraspberry.awards.dto.AwardsIntervalResponseDTO;
import com.goldenraspberry.awards.dto.ProducerIntervalDTO;
import com.goldenraspberry.awards.model.Movie;
import com.goldenraspberry.awards.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AwardsServiceIntegrationTest {

    @Autowired
    private AwardsService awardsService;

    @Autowired
    private MovieRepository movieRepository;

    @Test
    void testGetProducerIntervals() {
        // Verificar que há filmes vencedores no banco
        List<Movie> winningMovies = movieRepository.findByWinnerTrue();
        assertFalse(winningMovies.isEmpty(), "Deve haver pelo menos um filme vencedor no banco de dados");

        // Obter intervalos
        AwardsIntervalResponseDTO response = awardsService.getProducerIntervals();

        // Verificar que a resposta não é nula
        assertNotNull(response);
        assertNotNull(response.getMin());
        assertNotNull(response.getMax());

        // Verificar que há intervalos
        assertFalse(response.getMin().isEmpty(), "Deve haver pelo menos um intervalo mínimo");
        assertFalse(response.getMax().isEmpty(), "Deve haver pelo menos um intervalo máximo");

        // Verificar que todos os intervalos mínimos têm o mesmo valor
        if (!response.getMin().isEmpty()) {
            int minIntervalValue = response.getMin().get(0).getInterval();
            for (ProducerIntervalDTO dto : response.getMin()) {
                assertEquals(minIntervalValue, dto.getInterval(), 
                        "Todos os intervalos mínimos devem ter o mesmo valor");
            }
        }

        // Verificar que todos os intervalos máximos têm o mesmo valor
        if (!response.getMax().isEmpty()) {
            int maxIntervalValue = response.getMax().get(0).getInterval();
            for (ProducerIntervalDTO dto : response.getMax()) {
                assertEquals(maxIntervalValue, dto.getInterval(), 
                        "Todos os intervalos máximos devem ter o mesmo valor");
            }
        }

        // Verificar que o intervalo mínimo é menor ou igual ao máximo
        if (!response.getMin().isEmpty() && !response.getMax().isEmpty()) {
            int minInterval = response.getMin().get(0).getInterval();
            int maxInterval = response.getMax().get(0).getInterval();
            assertTrue(minInterval <= maxInterval, 
                    "Intervalo mínimo deve ser menor ou igual ao intervalo máximo");
        }

        // Verificar estrutura dos DTOs
        for (ProducerIntervalDTO dto : response.getMin()) {
            assertNotNull(dto.getProducer());
            assertNotNull(dto.getInterval());
            assertNotNull(dto.getPreviousWin());
            assertNotNull(dto.getFollowingWin());
            assertTrue(dto.getInterval() > 0);
            assertEquals(dto.getFollowingWin() - dto.getPreviousWin(), dto.getInterval());
        }

        for (ProducerIntervalDTO dto : response.getMax()) {
            assertNotNull(dto.getProducer());
            assertNotNull(dto.getInterval());
            assertNotNull(dto.getPreviousWin());
            assertNotNull(dto.getFollowingWin());
            assertTrue(dto.getInterval() > 0);
            assertEquals(dto.getFollowingWin() - dto.getPreviousWin(), dto.getInterval());
        }
    }
}

