package com.fox.aiagent.skills;

import java.io.Serializable;

/**
 * 技能执行结果封装
 * 统一成功/失败格式，便于统计和追踪
 *
 * @param <T> 结果数据类型
 */
public class SkillResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private final boolean success;

    /** 结果数据 */
    private final T data;

    /** 错误码 */
    private final String errorCode;

    /** 错误信息 */
    private final String errorMessage;

    /** 执行耗时(ms) */
    private final long costMs;

    /** 链路追踪ID */
    private final String traceId;

    /** 技能名称 */
    private final String skillName;

    private SkillResult(Builder<T> builder) {
        this.success = builder.success;
        this.data = builder.data;
        this.errorCode = builder.errorCode;
        this.errorMessage = builder.errorMessage;
        this.costMs = builder.costMs;
        this.traceId = builder.traceId;
        this.skillName = builder.skillName;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @SuppressWarnings("unchecked")
    public static <T> SkillResult<T> success(T data) {
        return SkillResult.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    public static <T> SkillResult<T> fail(String errorCode, String errorMessage) {
        return SkillResult.<T>builder()
            .success(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .build();
    }

    public static <T> SkillResult<T> timeout(String skillName) {
        return SkillResult.<T>builder()
            .success(false)
            .skillName(skillName)
            .errorCode("TIMEOUT")
            .errorMessage("Skill execution timeout: " + skillName)
            .build();
    }

    // Getters
    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public long getCostMs() { return costMs; }
    public String getTraceId() { return traceId; }
    public String getSkillName() { return skillName; }

    @Override
    public String toString() {
        if (success) {
            return String.format("SkillResult[success=true, skill=%s, cost=%dms, traceId=%s]",
                skillName, costMs, traceId);
        } else {
            return String.format("SkillResult[success=false, skill=%s, error=%s(%s), traceId=%s]",
                skillName, errorCode, errorMessage, traceId);
        }
    }

    public static class Builder<T> {
        private boolean success;
        private T data;
        private String errorCode;
        private String errorMessage;
        private long costMs;
        private String traceId;
        private String skillName;

        public Builder<T> success(boolean success) { this.success = success; return this; }
        public Builder<T> data(T data) { this.data = data; return this; }
        public Builder<T> errorCode(String errorCode) { this.errorCode = errorCode; return this; }
        public Builder<T> errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder<T> costMs(long costMs) { this.costMs = costMs; return this; }
        public Builder<T> traceId(String traceId) { this.traceId = traceId; return this; }
        public Builder<T> skillName(String skillName) { this.skillName = skillName; return this; }

        public SkillResult<T> build() { return new SkillResult<>(this); }
    }
}
