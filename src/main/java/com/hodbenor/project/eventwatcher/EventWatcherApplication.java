package com.hodbenor.project.eventwatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventWatcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventWatcherApplication.class, args);
	}
}