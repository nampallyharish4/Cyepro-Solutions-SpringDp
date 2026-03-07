package com.example.demo;

import com.example.demo.entity.Rule;
import com.example.demo.repository.RuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
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
	@Order(1)
	CommandLineRunner fixForeignKeys(DataSource dataSource) {
		return args -> {
			String[] badConstraints = {
				"ALTER TABLE ai_analysis DROP CONSTRAINT IF EXISTS fk8bgsryt6w6k6bgw1mxpfrm0o6",
				"ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS fkihx3rvcfyc1ym81lgqknqd0yv",
				"ALTER TABLE later_queue DROP CONSTRAINT IF EXISTS fk5mjvdhx3u2cemjytbm2bxwrhe"
			};
			try (Connection conn = dataSource.getConnection();
				 Statement stmt = conn.createStatement()) {
				for (String sql : badConstraints) {
					try {
						stmt.execute(sql);
						log.info("FK cleanup executed: {}", sql);
					} catch (Exception e) {
						log.debug("FK cleanup skipped ({}): {}", sql, e.getMessage());
					}
				}
			} catch (Exception e) {
				log.warn("FK cleanup failed: {}", e.getMessage());
			}
		};
	}

	@Bean
	@Order(2)
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
