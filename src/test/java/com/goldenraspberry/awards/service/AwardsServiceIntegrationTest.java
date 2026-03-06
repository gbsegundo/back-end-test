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

        // Verificar estrutura e consistência dos intervalos mínimos e máximos em loops otimizados
        int minIntervalValue = -1;
        int maxIntervalValue = -1;

        // Verificar intervalos mínimos: estrutura, consistência e valor único
        if (!response.getMin().isEmpty()) {
            minIntervalValue = response.getMin().get(0).getInterval();
            for (ProducerIntervalDTO dto : response.getMin()) {
                // Verificar estrutura
                assertNotNull(dto.getProducer(), "Producer não deve ser nulo");
                assertNotNull(dto.getInterval(), "Interval não deve ser nulo");
                assertNotNull(dto.getPreviousWin(), "PreviousWin não deve ser nulo");
                assertNotNull(dto.getFollowingWin(), "FollowingWin não deve ser nulo");
                assertTrue(dto.getInterval() > 0, "Interval deve ser maior que zero");
                assertEquals(dto.getFollowingWin() - dto.getPreviousWin(), dto.getInterval(),
                        "Interval deve ser a diferença entre followingWin e previousWin");
                // Verificar que todos têm o mesmo valor
                assertEquals(minIntervalValue, dto.getInterval(),
                        "Todos os intervalos mínimos devem ter o mesmo valor");
            }
        }

        // Verificar intervalos máximos: estrutura, consistência e valor único
        if (!response.getMax().isEmpty()) {
            maxIntervalValue = response.getMax().get(0).getInterval();
            for (ProducerIntervalDTO dto : response.getMax()) {
                // Verificar estrutura
                assertNotNull(dto.getProducer(), "Producer não deve ser nulo");
                assertNotNull(dto.getInterval(), "Interval não deve ser nulo");
                assertNotNull(dto.getPreviousWin(), "PreviousWin não deve ser nulo");
                assertNotNull(dto.getFollowingWin(), "FollowingWin não deve ser nulo");
                assertTrue(dto.getInterval() > 0, "Interval deve ser maior que zero");
                assertEquals(dto.getFollowingWin() - dto.getPreviousWin(), dto.getInterval(),
                        "Interval deve ser a diferença entre followingWin e previousWin");
                // Verificar que todos têm o mesmo valor
                assertEquals(maxIntervalValue, dto.getInterval(),
                        "Todos os intervalos máximos devem ter o mesmo valor");
            }
        }

        // Verificar que o intervalo mínimo é menor ou igual ao máximo
        if (minIntervalValue >= 0 && maxIntervalValue >= 0) {
            assertTrue(minIntervalValue <= maxIntervalValue,
                    "Intervalo mínimo deve ser menor ou igual ao intervalo máximo");
        }
    }
}

