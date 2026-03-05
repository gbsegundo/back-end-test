package com.goldenraspberry.awards.controller;

import com.goldenraspberry.awards.dto.AwardsIntervalResponseDTO;
import com.goldenraspberry.awards.service.AwardsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/awards")
public class AwardsController {

    @Autowired
    private AwardsService awardsService;

    @GetMapping("/intervals")
    public ResponseEntity<AwardsIntervalResponseDTO> getProducerIntervals() {
        AwardsIntervalResponseDTO response = awardsService.getProducerIntervals();
        return ResponseEntity.ok(response);
    }
}

