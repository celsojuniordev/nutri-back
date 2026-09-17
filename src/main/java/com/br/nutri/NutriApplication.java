package com.br.nutri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NutriApplication {

	public static void main(String[] args) {
		SpringApplication.run(NutriApplication.class, args);
	}

}
