package com.cyepro.engine.config;

import com.cyepro.engine.entity.Rule;
import com.cyepro.engine.repository.RuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RuleRepository ruleRepository;

    public DataSeeder(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    public void run(String... args) {
        seedFatigueRule();
    }

    private void seedFatigueRule() {
        Optional<Rule> existing = ruleRepository.findFirstByNameAndIsActiveTrueOrderByCreatedAtDesc("FATIGUE_LIMIT");
        if (existing.isPresent()) {
            Rule rule = existing.get();
            rule.setConditionValue("5");
            ruleRepository.save(rule);
            log.info("Fatigue limit rule updated!");
        } else {
            Rule rule = new Rule();
            rule.setName("FATIGUE_LIMIT");
            rule.setConditionType("system_setting");
            rule.setConditionValue("5");
            rule.setTargetPriority("SYSTEM");
            rule.setPriorityOrder(-1);
            ruleRepository.save(rule);
            log.info("Fatigue limit rule created!");
        }
    }
}
