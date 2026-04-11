package com.fox.aiagent.skills;

/**
 * 技能异常类
 */
public class SkillException extends RuntimeException {

    public SkillException(String message) {
        super(message);
    }

    public SkillException(String message, Throwable cause) {
        super(message, cause);
    }
}