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

@Service
public class AwardsService {

    @Autowired
    private MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public AwardsIntervalResponseDTO getProducerIntervals() {
        List<Movie> winningMovies = movieRepository.findByWinnerTrue();
        
        // Agrupar filmes vencedores por produtor
        Map<Producer, List<Integer>> producerWins = new HashMap<>();
        
        for (Movie movie : winningMovies) {
            for (Producer producer : movie.getProducers()) {
                producerWins.computeIfAbsent(producer, k -> new ArrayList<>())
                        .add(movie.getYear());
            }
        }

        // Calcular intervalos para cada produtor
        List<ProducerIntervalDTO> allIntervals = new ArrayList<>();
        
        for (Map.Entry<Producer, List<Integer>> entry : producerWins.entrySet()) {
            Producer producer = entry.getKey();
            List<Integer> years = entry.getValue();
            
            // Ordenar anos
            Collections.sort(years);
            
            // Calcular intervalos entre anos consecutivos
            for (int i = 0; i < years.size() - 1; i++) {
                int previousWin = years.get(i);
                int followingWin = years.get(i + 1);
                int interval = followingWin - previousWin;
                
                ProducerIntervalDTO dto = new ProducerIntervalDTO();
                dto.setProducer(producer.getName());
                dto.setInterval(interval);
                dto.setPreviousWin(previousWin);
                dto.setFollowingWin(followingWin);
                
                allIntervals.add(dto);
            }
        }

        // Encontrar intervalos mínimos e máximos
        if (allIntervals.isEmpty()) {
            return new AwardsIntervalResponseDTO(new ArrayList<>(), new ArrayList<>());
        }

        int minInterval = allIntervals.stream()
                .mapToInt(ProducerIntervalDTO::getInterval)
                .min()
                .orElse(0);

        int maxInterval = allIntervals.stream()
                .mapToInt(ProducerIntervalDTO::getInterval)
                .max()
                .orElse(0);

        List<ProducerIntervalDTO> minIntervals = allIntervals.stream()
                .filter(dto -> dto.getInterval().equals(minInterval))
                .collect(Collectors.toList());

        List<ProducerIntervalDTO> maxIntervals = allIntervals.stream()
                .filter(dto -> dto.getInterval().equals(maxInterval))
                .collect(Collectors.toList());

        return new AwardsIntervalResponseDTO(minIntervals, maxIntervals);
    }
}

