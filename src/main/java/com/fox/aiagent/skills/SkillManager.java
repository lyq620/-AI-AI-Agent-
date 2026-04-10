package com.fox.aiagent.skills;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 技能管理器，负责技能的注册、管理和执行
 */
@Component
public class SkillManager {

    private final Map<String, Skill> skills = new HashMap<>();

    /**
     * 注册技能
     * @param skill 要注册的技能
     */
    public void registerSkill(Skill skill) {
        Assert.notNull(skill, "Skill cannot be null");
        Assert.hasText(skill.getName(), "Skill name cannot be empty");
        skills.put(skill.getName(), skill);
    }

    /**
     * 获取技能
     * @param skillName 技能名称
     * @return 技能实例
     */
    public Optional<Skill> getSkill(String skillName) {
        return Optional.ofNullable(skills.get(skillName));
    }

    /**
     * 执行技能
     * @param skillName 技能名称
     * @param parameters 参数
     * @return 执行结果
     */
    public String executeSkill(String skillName, Map<String, Object> parameters) {
        Skill skill = skills.get(skillName);
        if (skill == null) {
            throw new IllegalArgumentException("Skill not found: " + skillName);
        }
        if (!skill.isAvailable()) {
            throw new IllegalStateException("Skill is not available: " + skillName);
        }
        return skill.execute(parameters);
    }

    /**
     * 获取所有可用技能
     * @return 所有技能
     */
    public Map<String, Skill> getAllSkills() {
        return new HashMap<>(skills);
    }

    /**
     * 检查技能是否存在
     * @param skillName 技能名称
     * @return 是否存在
     */
    public boolean hasSkill(String skillName) {
        return skills.containsKey(skillName);
    }
}