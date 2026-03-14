package com.example.orcamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
 import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
 @EnableScheduling
public class OrcamentoApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrcamentoApplication.class, args);
	}

}
