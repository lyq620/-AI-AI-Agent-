package com.fox.aiagent.skills;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * 技能配置类，自动注册所有技能
 */
@Configuration
public class SkillsConfig {

    private final SkillRegistrar skillRegistrar;

    @Autowired
    public SkillsConfig(SkillRegistrar skillRegistrar) {
        this.skillRegistrar = skillRegistrar;
    }

    /**
     * 自动注册所有技能
     */
    public void registerSkills() {
        // 在实际应用中，这里可以通过扫描@Component注解来自动发现技能
        // 这里我们手动注册技能，或者通过Spring的自动装配机制
    }
}