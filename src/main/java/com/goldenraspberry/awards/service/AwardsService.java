package com.goldenraspberry.awards.service;

import com.goldenraspberry.awards.dto.AwardsIntervalResponseDTO;
import com.goldenraspberry.awards.dto.ProducerIntervalDTO;
import com.goldenraspberry.awards.model.Movie;
import com.goldenraspberry.awards.model.Producer;
import com.goldenraspberry.awards.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class AwardsService {

    @Autowired
    private MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public AwardsIntervalResponseDTO getProducerIntervals() {
        List<Movie> winningMovies = movieRepository.findByWinnerTrue();
        
        // Agrupar anos de vitórias por produtor em uma única passada
        Map<String, List<Integer>> producerWins = winningMovies.stream()
                .flatMap(movie -> movie.getProducers().stream()
                        .map(producer -> new AbstractMap.SimpleEntry<>(producer.getName(), movie.getYear())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        // Calcular todos os intervalos e encontrar min/max em uma única passada
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
        
        // Primeira passada: encontrar min e max
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

        return new AwardsIntervalResponseDTO(minIntervals, maxIntervals);
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
}

