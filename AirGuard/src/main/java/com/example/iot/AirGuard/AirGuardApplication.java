package com.example.iot.AirGuard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AirGuardApplication {

	public static void main(String[] args) {
		SpringApplication.run(AirGuardApplication.class, args);
	}

}
