//package com.example.orcamento.controller;
//
//import com.example.orcamento.dto.GastoRecorrenteCartaoDTO;
//import com.example.orcamento.dto.PrevisaoGastoCartaoDTO;
//import com.example.orcamento.dto.SugestaoEconomiaCartaoDTO;
//import com.example.orcamento.service.AnaliseCartaoService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/cartoes/analise")
//@CrossOrigin(origins = "*")
//public class AnaliseCartaoController {
//
//    @Autowired
//    private AnaliseCartaoService analiseCartaoService;
//
//    @GetMapping("/recorrentes")
//    public ResponseEntity<List<GastoRecorrenteCartaoDTO>> getGastosRecorrentes() {
//        return ResponseEntity.ok(analiseCartaoService.getGastosRecorrentes());
//    }
//
//    @GetMapping("/sugestoes")
//    public ResponseEntity<List<SugestaoEconomiaCartaoDTO>> getSugestoesEconomia() {
//        return ResponseEntity.ok(analiseCartaoService.getSugestoesEconomia());
//    }
//
//    @GetMapping("/previsoes")
//    public ResponseEntity<List<PrevisaoGastoCartaoDTO>> getPrevisoes() {
//        return ResponseEntity.ok(analiseCartaoService.getPrevisoes());
//    }
//}