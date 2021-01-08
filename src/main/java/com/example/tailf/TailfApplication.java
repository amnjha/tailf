package com.example.tailf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@SpringBootApplication
public class TailfApplication {

	public static void main(String[] args) {
		SpringApplication.run(TailfApplication.class, args);
	}

	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

		scheduler.setPoolSize(100);
		scheduler.setThreadNamePrefix("scheduled-task-");
		scheduler.setDaemon(true);

		return scheduler;
	}
}
