package com.fox.aiagent.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 技能管理器，负责技能的注册、管理和执行
 * 支持动态技能选择，解决 Token 爆炸问题
 * 支持超时控制、链路追踪、执行统计
 */
@Component
public class SkillManager {

    private static final Logger logger = LoggerFactory.getLogger(SkillManager.class);

    /** 技能注册表 */
    private final Map<String, Skill> skills = new HashMap<>();

    /** 技能执行统计 */
    private final Map<String, SkillMetrics> metrics = new ConcurrentHashMap<>();

    /** 链路追踪管理器 */
    private final SkillTraceManager traceManager;

    /** 执行线程池 */
    private final ExecutorService executorService;

    /** 默认超时时间(ms) */
    private static final long DEFAULT_TIMEOUT_MS = 30000;

    /** Skill 到 ToolCallback 的转换器（在 LoveApp 中注入） */
    private SkillToolCallbackProvider skillToolCallbackProvider;

    /**
     * 设置 ToolCallback 转换器（延迟注入，避免循环依赖）
     */
    public void setSkillToolCallbackProvider(SkillToolCallbackProvider provider) {
        this.skillToolCallbackProvider = provider;
    }

    // 构造函数
    public SkillManager(SkillTraceManager traceManager) {
        this.traceManager = traceManager;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "SkillExecutor");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 注册技能
     * @param skill 要注册的技能
     */
    public void registerSkill(Skill skill) {
        Assert.notNull(skill, "Skill cannot be null");
        Assert.hasText(skill.getName(), "Skill name cannot be empty");
        skills.put(skill.getName(), skill);
        metrics.put(skill.getName(), new SkillMetrics(skill.getName()));
        logger.info("【SkillManager】注册技能: {}", skill.getName());
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
     * 执行技能（带超时控制、链路追踪、统计）
     * @param skillName 技能名称
     * @param parameters 参数
     * @return 执行结果封装
     */
    public SkillResult<String> executeSkill(String skillName, Map<String, Object> parameters) {
        return executeSkill(skillName, parameters, DEFAULT_TIMEOUT_MS);
    }

    /**
     * 执行技能（带超时控制）
     * @param skillName 技能名称
     * @param parameters 参数
     * @param timeoutMs 超时时间(ms)
     * @return 执行结果封装
     */
    public SkillResult<String> executeSkill(String skillName, Map<String, Object> parameters, long timeoutMs) {
        // 1. 参数校验
        Skill skill = skills.get(skillName);
        if (skill == null) {
            logger.error("【SkillManager】技能不存在: {}", skillName);
            return SkillResult.fail("SKILL_NOT_FOUND", "Skill not found: " + skillName);
        }

        if (!skill.isAvailable()) {
            logger.error("【SkillManager】技能不可用: {}", skillName);
            return SkillResult.fail("SKILL_NOT_AVAILABLE", "Skill is not available: " + skillName);
        }

        // 2. 开始链路追踪
        SkillExecutionChain chain = traceManager.startTrace(skillName, parameters);

        // 3. 参数校验
        try {
            skill.validate(parameters);
        } catch (SkillValidationException e) {
            chain.markValidationError(e.getMessage());
            getMetrics(skillName).recordValidationFail();
            logger.warn("【SkillManager】参数校验失败: {}", e.getMessage());
            return SkillResult.<String>builder()
                .success(false)
                .skillName(skillName)
                .traceId(chain.getTraceId())
                .errorCode("VALIDATION_ERROR")
                .errorMessage(e.getMessage())
                .costMs(chain.getCostMs())
                .build();
        }

        // 4. 异步执行（带超时控制）
        Future<String> future = null;
        try {
            future = executorService.submit(() -> {
                try {
                    String result = skill.execute(parameters);
                    chain.markSuccess(result);
                    return result;
                } catch (Exception e) {
                    chain.markFail("EXECUTION_ERROR", e.getMessage());
                    throw e;
                }
            });

            String result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            getMetrics(skillName).recordSuccess(chain.getCostMs());
            logger.info("【SkillManager】技能执行成功: {}, cost={}ms, traceId={}", skillName, chain.getCostMs(), chain.getTraceId());

            return SkillResult.<String>builder()
                .success(true)
                .skillName(skillName)
                .traceId(chain.getTraceId())
                .data(result)
                .costMs(chain.getCostMs())
                .build();

        } catch (TimeoutException e) {
            if (future != null) {
                future.cancel(true);
            }
            chain.markTimeout();
            getMetrics(skillName).recordTimeout();
            logger.error("【SkillManager】技能执行超时: {}, timeout={}ms", skillName, timeoutMs);
            return SkillResult.timeout(skillName);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            chain.markFail("INTERRUPTED", e.getMessage());
            getMetrics(skillName).recordFail(0);
            return SkillResult.fail("INTERRUPTED", "Skill execution interrupted");

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            chain.markFail("EXECUTION_ERROR", cause.getMessage());
            getMetrics(skillName).recordFail(chain.getCostMs());
            logger.error("【SkillManager】技能执行异常: {}", skillName, cause);
            return SkillResult.<String>builder()
                .success(false)
                .skillName(skillName)
                .traceId(chain.getTraceId())
                .errorCode("EXECUTION_ERROR")
                .errorMessage(cause.getMessage())
                .costMs(chain.getCostMs())
                .build();
        }
    }

    /**
     * 执行技能（简化版，返回字符串）
     * @param skillName 技能名称
     * @param parameters 参数
     * @return 执行结果字符串
     */
    public String executeSkillSimple(String skillName, Map<String, Object> parameters) {
        SkillResult<String> result = executeSkill(skillName, parameters);
        if (result.isSuccess()) {
            return result.getData();
        } else {
            throw new SkillException("Skill execution failed: " + result.getErrorMessage());
        }
    }

    /**
     * 执行技能（通过意图选择，批量执行）
     * @param skillNames 技能名称列表
     * @param parameters 参数
     * @return 执行结果Map
     */
    public Map<String, SkillResult<String>> executeSkills(List<String> skillNames, Map<String, Object> parameters) {
        Map<String, SkillResult<String>> results = new ConcurrentHashMap<>();

        // 并行执行所有技能
        List<CompletableFuture<Void>> futures = skillNames.stream()
            .map(skillName -> CompletableFuture.runAsync(() -> {
                results.put(skillName, executeSkill(skillName, parameters));
            }, executorService))
            .toList();

        // 等待所有完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return results;
    }

    /**
     * 根据意图获取相关的技能名称列表
     * @param intent 意图名称
     * @return 相关的技能名称列表
     */
    public List<String> getSkillNamesByIntent(String intent) {
        List<String> result = new ArrayList<>();
        switch (intent) {
            case "约会计划":
                result.add("date_plan");
                result.add("date_budget");
                break;
            case "预算规划":
                result.add("date_budget");
                break;
            case "恋爱咨询":
                result.add("love_knowledge");
                break;
            case "通用咨询":
            default:
                // 返回所有技能
                result.addAll(skills.keySet());
                break;
        }
        logger.info("【SkillManager】意图「{}」匹配到技能: {}", intent, result);
        return result;
    }

    /**
     * 根据技能名称获取对应的 ToolCallback 数组
     * 只返回相关的技能，减少 Token 消耗
     * @param skillNames 技能名称列表
     * @return ToolCallback 数组
     */
    public ToolCallback[] getToolCallbacksBySkillNames(List<String> skillNames) {
        if (skillToolCallbackProvider == null) {
            logger.warn("【SkillManager】SkillToolCallbackProvider 未设置，返回空数组");
            return new ToolCallback[0];
        }

        // 获取所有 ToolCallback
        ToolCallback[] allCallbacks = skillToolCallbackProvider.getToolCallbacks();

        // 过滤出相关的
        List<ToolCallback> filtered = new ArrayList<>();
        for (ToolCallback callback : allCallbacks) {
            String toolName = callback.getToolDefinition().name();
            if (skillNames.contains(toolName)) {
                filtered.add(callback);
            }
        }

        logger.info("【SkillManager】根据技能列表 {} 获取到 {} 个 ToolCallback", skillNames, filtered.size());
        return filtered.toArray(new ToolCallback[0]);
    }

    /**
     * 根据意图获取对应的 ToolCallback 数组
     * 这是解决 Token 爆炸问题的核心方法
     * @param intent 意图名称
     * @return ToolCallback 数组
     */
    public ToolCallback[] getToolCallbacksByIntent(String intent) {
        List<String> skillNames = getSkillNamesByIntent(intent);
        return getToolCallbacksBySkillNames(skillNames);
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

    /**
     * 获取技能统计指标
     * @param skillName 技能名称
     * @return 统计指标
     */
    public SkillMetrics getMetrics(String skillName) {
        return metrics.computeIfAbsent(skillName, SkillMetrics::new);
    }

    /**
     * 获取所有技能的统计指标
     * @return 统计指标Map
     */
    public Map<String, SkillMetrics> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    /**
     * 获取技能执行链路
     * @param traceId 链路ID
     * @return 链路信息
     */
    public SkillExecutionChain getExecutionChain(String traceId) {
        return traceManager.getTrace(traceId);
    }

    /**
     * 获取最近的执行链路
     * @param count 数量
     * @return 链路列表
     */
    public List<SkillExecutionChain> getRecentChains(int count) {
        return traceManager.getRecentTraces(count);
    }

    /**
     * 获取追踪统计
     * @return 统计Map
     */
    public Map<String, Integer> getTraceStats() {
        return traceManager.getTraceStats();
    }
}
