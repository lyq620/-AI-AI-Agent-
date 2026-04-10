package com.fox.aiagent.skills;

/**
 * 技能参数校验异常
 * 当技能接收到非法参数时抛出
 */
public class SkillValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 技能名称 */
    private final String skillName;

    /** 错误的参数名 */
    private final String paramName;

    /** 错误参数值 */
    private final Object paramValue;

    public SkillValidationException(String message) {
        super(message);
        this.skillName = null;
        this.paramName = null;
        this.paramValue = null;
    }

    public SkillValidationException(String skillName, String paramName, Object paramValue, String reason) {
        super(String.format("参数校验失败 - skill=%s, param=%s, value=%s, reason=%s",
            skillName, paramName, paramValue, reason));
        this.skillName = skillName;
        this.paramName = paramName;
        this.paramValue = paramValue;
    }

    public SkillValidationException(String message, Throwable cause) {
        super(message, cause);
        this.skillName = null;
        this.paramName = null;
        this.paramValue = null;
    }

    public String getSkillName() { return skillName; }
    public String getParamName() { return paramName; }
    public Object getParamValue() { return paramValue; }
}
