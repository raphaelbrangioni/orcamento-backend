package com.example.orcamento.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dados-dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class DadosDashboardDTO {
}
