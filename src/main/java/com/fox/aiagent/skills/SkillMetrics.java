package com.fox.aiagent.skills;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 技能执行统计指标
 * 用于监控技能使用情况和性能
 */
public class SkillMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 技能名称 */
    private final String skillName;

    /** 总调用次数 */
    private final AtomicLong totalCalls = new AtomicLong(0);

    /** 成功次数 */
    private final AtomicLong successCalls = new AtomicLong(0);

    /** 失败次数 */
    private final AtomicLong failCalls = new AtomicLong(0);

    /** 超时次数 */
    private final AtomicLong timeoutCalls = new AtomicLong(0);

    /** 校验失败次数 */
    private final AtomicLong validationFailCalls = new AtomicLong(0);

    /** 总耗时(ms) */
    private final AtomicLong totalCostMs = new AtomicLong(0);

    /** 最大耗时(ms) */
    private volatile long maxCostMs = 0;

    /** 最小耗时(ms) */
    private volatile long minCostMs = Long.MAX_VALUE;

    public SkillMetrics(String skillName) {
        this.skillName = skillName;
    }

    /**
     * 记录成功执行
     */
    public void recordSuccess(long costMs) {
        totalCalls.incrementAndGet();
        successCalls.incrementAndGet();
        updateCostStats(costMs);
    }

    /**
     * 记录失败执行
     */
    public void recordFail(long costMs) {
        totalCalls.incrementAndGet();
        failCalls.incrementAndGet();
        updateCostStats(costMs);
    }

    /**
     * 记录超时
     */
    public void recordTimeout() {
        totalCalls.incrementAndGet();
        timeoutCalls.incrementAndGet();
    }

    /**
     * 记录校验失败
     */
    public void recordValidationFail() {
        validationFailCalls.incrementAndGet();
    }

    private void updateCostStats(long costMs) {
        totalCostMs.addAndGet(costMs);
        if (costMs > maxCostMs) {
            maxCostMs = costMs;
        }
        if (costMs < minCostMs) {
            minCostMs = costMs;
        }
    }

    /**
     * 获取平均耗时(ms)
     */
    public double getAvgCostMs() {
        long total = successCalls.get() + failCalls.get();
        if (total == 0) return 0;
        return (double) totalCostMs.get() / total;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        long total = totalCalls.get();
        if (total == 0) return 0;
        return (double) successCalls.get() / total;
    }

    // Getters
    public String getSkillName() { return skillName; }
    public long getTotalCalls() { return totalCalls.get(); }
    public long getSuccessCalls() { return successCalls.get(); }
    public long getFailCalls() { return failCalls.get(); }
    public long getTimeoutCalls() { return timeoutCalls.get(); }
    public long getValidationFailCalls() { return validationFailCalls.get(); }
    public long getTotalCostMs() { return totalCostMs.get(); }
    public long getMaxCostMs() { return maxCostMs == Long.MAX_VALUE ? 0 : maxCostMs; }
    public long getMinCostMs() { return minCostMs == Long.MAX_VALUE ? 0 : minCostMs; }

    @Override
    public String toString() {
        return String.format(
            "SkillMetrics[name=%s, total=%d, success=%d(%.1f%%), fail=%d, timeout=%d, validationFail=%d, avgCost=%.2fms, minCost=%dms, maxCost=%dms]",
            skillName, totalCalls.get(), successCalls.get(), getSuccessRate() * 100,
            failCalls.get(), timeoutCalls.get(), validationFailCalls.get(),
            getAvgCostMs(), getMinCostMs(), getMaxCostMs()
        );
    }
}
