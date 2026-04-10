package com.fox.aiagent.skills;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 技能执行链路追踪
 * 用于问题定位和执行审计
 */
public class SkillExecutionChain implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 链路追踪ID */
    private final String traceId;

    /** 技能名称 */
    private final String skillName;

    /** 输入参数 */
    private final Map<String, Object> inputParams;

    /** 输出结果 */
    private String outputResult;

    /** 开始时间 */
    private final long startTime;

    /** 结束时间 */
    private long endTime;

    /** 执行状态：SUCCESS / FAIL / TIMEOUT / VALIDATION_ERROR */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 错误码 */
    private String errorCode;

    public SkillExecutionChain(String traceId, String skillName, Map<String, Object> inputParams) {
        this.traceId = traceId;
        this.skillName = skillName;
        this.inputParams = new HashMap<>(inputParams);
        this.startTime = System.currentTimeMillis();
        this.status = "RUNNING";
    }

    /**
     * 执行成功
     */
    public void markSuccess(String output) {
        this.outputResult = output;
        this.endTime = System.currentTimeMillis();
        this.status = "SUCCESS";
    }

    /**
     * 执行失败
     */
    public void markFail(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.endTime = System.currentTimeMillis();
        this.status = "FAIL";
    }

    /**
     * 执行超时
     */
    public void markTimeout() {
        this.errorCode = "TIMEOUT";
        this.errorMessage = "Skill execution timeout";
        this.endTime = System.currentTimeMillis();
        this.status = "TIMEOUT";
    }

    /**
     * 参数校验失败
     */
    public void markValidationError(String errorMessage) {
        this.errorCode = "VALIDATION_ERROR";
        this.errorMessage = errorMessage;
        this.endTime = System.currentTimeMillis();
        this.status = "VALIDATION_ERROR";
    }

    /**
     * 获取执行耗时(ms)
     */
    public long getCostMs() {
        if (endTime == 0) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }

    // Getters
    public String getTraceId() { return traceId; }
    public String getSkillName() { return skillName; }
    public Map<String, Object> getInputParams() { return inputParams; }
    public String getOutputResult() { return outputResult; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public String getErrorCode() { return errorCode; }

    @Override
    public String toString() {
        return String.format(
            "SkillExecutionChain[traceId=%s, skill=%s, status=%s, cost=%dms, startTime=%s, error=%s]",
            traceId, skillName, status, getCostMs(),
            LocalDateTime.now().toString(), errorMessage
        );
    }
}
