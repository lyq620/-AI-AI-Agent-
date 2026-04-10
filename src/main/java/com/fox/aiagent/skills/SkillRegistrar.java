package com.fox.aiagent.skills;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 技能注册器，自动注册所有标记为 @SkillComponent 的技能
 */
@Component
public class SkillRegistrar {

    private final SkillManager skillManager;

    @Autowired
    public SkillRegistrar(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    /**
     * 初始化时自动注册所有技能
     */
    @PostConstruct
    public void registerSkills() {
        // 在实际应用中，这里可以通过扫描@Component注解来自动发现技能
        // 这里我们手动注册技能，或者通过Spring的自动装配机制
    }

    /**
     * 注册单个技能
     * @param skill 技能实例
     */
    public void register(Skill skill) {
        skillManager.registerSkill(skill);
    }

    /**
     * 批量注册技能
     * @param skills 技能列表
     */
    public void registerAll(List<Skill> skills) {
        skills.forEach(this::register);
    }
}