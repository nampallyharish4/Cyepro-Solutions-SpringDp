package com.example.demo.rules;

import com.example.demo.entity.Event;
import com.example.demo.entity.Rule;
import com.example.demo.repository.RuleRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RuleEngine {
    private final RuleRepository ruleRepository;

    public RuleEngine(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public Optional<RuleEvaluationResult> evaluate(Event event) {
// ...existing code...
        List<Rule> enabledRules = ruleRepository.findByEnabledTrueOrderByPriorityDesc();
        for (Rule rule : enabledRules) {
            if (evaluateCondition(rule.getConditionJson(), event)) {
                return Optional.of(new RuleEvaluationResult(rule.getAction(), "Rule " + rule.getName() + " matched", rule));
            }
        }
        return Optional.empty();
    }

    private boolean evaluateCondition(Map<String, Object> condition, Event event) {
        // Simplified condition matching:
        // { "field": "event_type", "op": "equals", "value": "SECURITY" }
        String field = (String) condition.get("field");
        String op = (String) condition.get("op");
        Object value = condition.get("value");

        if ("event_type".equals(field)) {
            return matches(event.getEventType(), op, value);
        } else if ("priority_hint".equals(field)) {
            return matches(event.getPriorityHint(), op, value);
        } else if ("source".equals(field)) {
            return matches(event.getSource(), op, value);
        }
        return false;
    }

    private boolean matches(String actual, String op, Object expected) {
        if ("equals".equals(op)) {
            return actual.equalsIgnoreCase(String.valueOf(expected));
        } else if ("contains".equals(op)) {
            return actual.toLowerCase().contains(String.valueOf(expected).toLowerCase());
        }
        return false;
    }

    public record RuleEvaluationResult(String action, String reason, Rule rule) {}
}
