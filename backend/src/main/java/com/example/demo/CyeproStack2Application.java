package com.example.demo;

import com.example.demo.entity.Rule;
import com.example.demo.repository.RuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
@Slf4j
public class CyeproStack2Application {

	public static void main(String[] args) {
		SpringApplication.run(CyeproStack2Application.class, args);
	}

	@Bean
	CommandLineRunner initRules(RuleRepository ruleRepository) {
		return args -> {
			if (ruleRepository.count() == 0) {
				log.info("Seeding initial rules into the engine...");
				ruleRepository.save(Rule.builder()
						.name("High Priority Critical")
						.conditionJson(Map.of("field", "priority_hint", "op", "equals", "value", "HIGH"))
						.action("NOW")
						.priority(100)
						.enabled(true)
						.build());

				ruleRepository.save(Rule.builder()
						.name("Security Event")
						.conditionJson(Map.of("field", "event_type", "op", "contains", "value", "SECURITY"))
						.action("NOW")
						.priority(80)
						.enabled(true)
						.build());

				ruleRepository.save(Rule.builder()
						.name("Generic Low Priority")
						.conditionJson(Map.of("field", "priority_hint", "op", "equals", "value", "LOW"))
						.action("NEVER")
						.priority(10)
						.enabled(true)
						.build());
				log.info("Successfully seeded 3 default rules.");
			}
		};
	}
}
