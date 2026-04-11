package com.fox.aiagent.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 技能执行链路追踪管理器
 * 统一管理所有技能执行的链路追踪
 */
@Component
public class SkillTraceManager {

    private static final Logger logger = LoggerFactory.getLogger(SkillTraceManager.class);

    /** 链路追踪ID生成器 */
    private static final ConcurrentMap<String, SkillExecutionChain> executionChains = new ConcurrentHashMap<>();

    /** 最大保存的链路数量 */
    private static final int MAX_TRACE_SIZE = 1000;

    /** 生成追踪ID */
    public String generateTraceId() {
        return "SKILL-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

    /**
     * 开始追踪
     */
    public SkillExecutionChain startTrace(String skillName, Map<String, Object> params) {
        String traceId = generateTraceId();
        SkillExecutionChain chain = new SkillExecutionChain(traceId, skillName, params);
        executionChains.put(traceId, chain);

        // 超过上限时清理旧链路
        if (executionChains.size() > MAX_TRACE_SIZE) {
            cleanupOldTraces();
        }

        logger.debug("【SkillTrace】开始追踪: traceId={}, skill={}", traceId, skillName);
        return chain;
    }

    /**
     * 获取链路
     */
    public SkillExecutionChain getTrace(String traceId) {
        return executionChains.get(traceId);
    }

    /**
     * 获取所有链路
     */
    public List<SkillExecutionChain> getAllTraces() {
        return new ArrayList<>(executionChains.values());
    }

    /**
     * 获取最近的N条链路
     */
    public List<SkillExecutionChain> getRecentTraces(int count) {
        return executionChains.values().stream()
            .sorted((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()))
            .limit(count)
            .toList();
    }

    /**
     * 清理过期链路
     */
    private void cleanupOldTraces() {
        // 保留最近的100条
        List<SkillExecutionChain> recent = getRecentTraces(100);
        executionChains.clear();
        recent.forEach(chain -> executionChains.put(chain.getTraceId(), chain));
    }

    /**
     * 清理指定链路
     */
    public void removeTrace(String traceId) {
        executionChains.remove(traceId);
    }

    /**
     * 获取链路统计
     */
    public Map<String, Integer> getTraceStats() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        stats.put("total", executionChains.size());
        stats.put("running", (int) executionChains.values().stream().filter(c -> "RUNNING".equals(c.getStatus())).count());
        stats.put("success", (int) executionChains.values().stream().filter(c -> "SUCCESS".equals(c.getStatus())).count());
        stats.put("fail", (int) executionChains.values().stream().filter(c -> "FAIL".equals(c.getStatus())).count());
        return stats;
    }
}
