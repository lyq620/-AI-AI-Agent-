package com.fox.ai.agent.skill.core;

import java.util.List;
import java.util.Map;

/**
 * Skills 基础接口
 */
public interface ISkill {
    
    /**
     * 技能唯一标识
     */
    String getSkillId();
    
    /**
     * 技能名称
     */
    String getSkillName();
    
    /**
     * 技能描述
     */
    String getDescription();
    
    /**
     * 技能版本
     */
    String getVersion();
    
    /**
     * 获取技能所需的参数定义
     */
    List<SkillParameter> getParameters();
    
    /**
     * 执行技能
     */
    SkillResult execute(SkillContext context);
    
    /**
     * 验证输入参数
     */
    boolean validateInput(Map<String, Object> input);
    
    /**
     * 技能是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取技能标签
     */
    List<String> getTags();
    
    /**
     * 技能依赖
     */
    List<String> getDependencies();
}