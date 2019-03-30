package de.grimmpp.cloudFoundry.resourceScheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppManagerApplication.class, args);
	}

}
