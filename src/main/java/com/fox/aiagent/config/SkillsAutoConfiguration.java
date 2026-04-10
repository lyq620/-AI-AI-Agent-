package com.fox.aiagent.config;

import com.fox.aiagent.skills.DateBudgetSkill;
import com.fox.aiagent.skills.DatePlanSkill;
import com.fox.aiagent.skills.LoveKnowledgeSkill;
import com.fox.aiagent.skills.SkillRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Skills 自动配置类，自动注册所有技能
 */
@Configuration
public class SkillsAutoConfiguration {

    private final SkillRegistrar skillRegistrar;

    @Autowired
    public SkillsAutoConfiguration(SkillRegistrar skillRegistrar) {
        this.skillRegistrar = skillRegistrar;
    }

    /**
     * 自动注册所有技能
     */
    public void registerSkills() {
        // 注册 LoveKnowledgeSkill
        skillRegistrar.register(new LoveKnowledgeSkill(
            null, // ChatClient 会在运行时注入
            null, // QueryRewriter 会在运行时注入
            null  // LoveAppRagCustomAdvisorFactory 会在运行时注入
        ));

        // 注册其他技能
        skillRegistrar.register(new DatePlanSkill(null));
        skillRegistrar.register(new DateBudgetSkill(null));
        // ImageSearchSkill 需要在运行时注入依赖，暂时注释掉
        // skillRegistrar.register(new ImageSearchSkill(null, null));
    }
}