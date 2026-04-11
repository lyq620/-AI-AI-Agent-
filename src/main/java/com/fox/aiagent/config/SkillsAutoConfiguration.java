package com.fox.aiagent.config;

import com.fox.aiagent.skills.SkillRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Skills 自动配置类
 * 技能自动注册由 SkillRegistrar 的 @PostConstruct 完成
 * 所有带有 @SkillComponent 注解的 Skill 实现类都会被自动发现并注册
 */
@Configuration
public class SkillsAutoConfiguration {

    private final SkillRegistrar skillRegistrar;

    public SkillsAutoConfiguration(SkillRegistrar skillRegistrar) {
        this.skillRegistrar = skillRegistrar;
    }
}