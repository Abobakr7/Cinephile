package com.example.cinephile;

import org.springframework.boot.SpringApplication;

public class TestCinephileApplication {

	public static void main(String[] args) {
		SpringApplication.from(CinephileApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
