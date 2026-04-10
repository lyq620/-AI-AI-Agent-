package com.fox.aiagent.skills;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 技能健康检查
 * 实现 Spring Boot Actuator 的 HealthIndicator 接口
 * 用于监控技能系统状态
 */
@Component("skillsHealth")
public class SkillHealthIndicator implements HealthIndicator {

    private final SkillManager skillManager;

    public SkillHealthIndicator(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @Override
    public Health health() {
        Map<String, Skill> allSkills = skillManager.getAllSkills();

        if (allSkills.isEmpty()) {
            return Health.down()
                .withDetail("error", "No skills registered")
                .build();
        }

        int totalSkills = allSkills.size();
        int healthySkills = 0;
        int unhealthySkills = 0;

        StringBuilder details = new StringBuilder();

        for (Map.Entry<String, Skill> entry : allSkills.entrySet()) {
            String skillName = entry.getKey();
            Skill skill = entry.getValue();

            if (skill.isAvailable()) {
                healthySkills++;
            } else {
                unhealthySkills++;
                details.append(skillName).append("=UNAVAILABLE; ");
            }
        }

        // 检查执行统计
        Map<String, SkillMetrics> allMetrics = skillManager.getAllMetrics();
        long totalCalls = 0;
        long failedCalls = 0;

        for (SkillMetrics metrics : allMetrics.values()) {
            totalCalls += metrics.getTotalCalls();
            failedCalls += metrics.getFailCalls() + metrics.getTimeoutCalls();
        }

        double errorRate = totalCalls > 0 ? (double) failedCalls / totalCalls : 0;

        Health.Builder builder = healthySkills == totalSkills ? Health.up() : Health.down();

        return builder
            .withDetail("totalSkills", totalSkills)
            .withDetail("healthySkills", healthySkills)
            .withDetail("unhealthySkills", unhealthySkills)
            .withDetail("totalCalls", totalCalls)
            .withDetail("failedCalls", failedCalls)
            .withDetail("errorRate", String.format("%.2f%%", errorRate * 100))
            .withDetail("skills", allSkills.keySet())
            .build();
    }
}
