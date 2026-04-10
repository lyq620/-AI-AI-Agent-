package com.fox.aiagent.skills;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 技能意图分类器
 * 根据用户消息的关键词/语义，判断用户需要哪个技能
 *
 * 解决 Token 爆炸问题：不在每次请求时发送所有技能的完整 schema
 * 而是先识别意图，再只加载相关的技能 schema
 */
@Component
public class SkillIntentClassifier {

    /**
     * 技能意图定义
     */
    public static class SkillIntent {
        /** 意图名称 */
        private final String name;
        /** 意图描述 */
        private final String description;
        /** 关联的技能名称列表 */
        private final List<String> relatedSkills;

        public SkillIntent(String name, String description, List<String> relatedSkills) {
            this.name = name;
            this.description = description;
            this.relatedSkills = relatedSkills;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<String> getRelatedSkills() { return relatedSkills; }
    }

    /**
     * 预定义的意图及其关联技能
     * 可以根据实际业务扩展更多意图
     */
    private static final List<SkillIntent> DEFINED_INTENTS = Arrays.asList(
        new SkillIntent(
            "约会计划",
            "用户想要规划约会、安排行程",
            Arrays.asList("date_plan", "date_budget")
        ),
        new SkillIntent(
            "预算规划",
            "用户询问约会花费、费用预算",
            Arrays.asList("date_budget")
        ),
        new SkillIntent(
            "恋爱咨询",
            "用户询问恋爱问题、感情困惑",
            Arrays.asList("love_knowledge")
        ),
        new SkillIntent(
            "通用咨询",
            "用户的请求不明确，需要多技能协作",
            Arrays.asList("date_plan", "date_budget", "love_knowledge")
        )
    );

    /**
     * 意图关键词映射
     * 用户消息包含这些关键词时，触发对应意图
     */
    private static final Map<String, String> INTENT_KEYWORDS = new HashMap<>();

    static {
        // 约会计划意图关键词
        INTENT_KEYWORDS.put("约会", "约会计划");
        INTENT_KEYWORDS.put("安排", "约会计划");
        INTENT_KEYWORDS.put("规划", "约会计划");
        INTENT_KEYWORDS.put("行程", "约会计划");
        INTENT_KEYWORDS.put("去哪里", "约会计划");
        INTENT_KEYWORDS.put("推荐地点", "约会计划");
        INTENT_KEYWORDS.put("浪漫", "约会计划");
        INTENT_KEYWORDS.put("晚餐", "约会计划");
        INTENT_KEYWORDS.put("电影", "约会计划");

        // 预算规划意图关键词
        INTENT_KEYWORDS.put("预算", "预算规划");
        INTENT_KEYWORDS.put("花费", "预算规划");
        INTENT_KEYWORDS.put("费用", "预算规划");
        INTENT_KEYWORDS.put("钱", "预算规划");
        INTENT_KEYWORDS.put("多少钱", "预算规划");
        INTENT_KEYWORDS.put("开销", "预算规划");
        INTENT_KEYWORDS.put("分配", "预算规划");

        // 恋爱咨询意图关键词
        INTENT_KEYWORDS.put("恋爱", "恋爱咨询");
        INTENT_KEYWORDS.put("感情", "恋爱咨询");
        INTENT_KEYWORDS.put("追", "恋爱咨询");
        INTENT_KEYWORDS.put("表白", "恋爱咨询");
        INTENT_KEYWORDS.put("吵架", "恋爱咨询");
        INTENT_KEYWORDS.put("分手", "恋爱咨询");
        INTENT_KEYWORDS.put("女朋友", "恋爱咨询");
        INTENT_KEYWORDS.put("男朋友", "恋爱咨询");
        INTENT_KEYWORDS.put("另一半", "恋爱咨询");
    }

    /**
     * 根据用户消息识别意图
     *
     * @param userMessage 用户消息
     * @return 匹配的意图列表（按相关性排序）
     */
    public List<SkillIntent> classifyIntent(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            // 空消息返回通用咨询
            return Collections.singletonList(findIntentByName("通用咨询"));
        }

        String lowerMessage = userMessage.toLowerCase();
        Map<String, Integer> intentScore = new HashMap<>();

        // 遍历意图关键词，检查消息中是否包含
        for (Map.Entry<String, String> entry : INTENT_KEYWORDS.entrySet()) {
            String keyword = entry.getKey();
            String intentName = entry.getValue();

            if (lowerMessage.contains(keyword.toLowerCase())) {
                // 匹配到关键词，加意图分数
                intentScore.merge(intentName, 1, Integer::sum);
            }
        }

        // 如果没有匹配到任何意图，返回通用咨询
        if (intentScore.isEmpty()) {
            return Collections.singletonList(findIntentByName("通用咨询"));
        }

        // 按分数排序，返回匹配的意图
        List<SkillIntent> matchedIntents = new ArrayList<>();
        intentScore.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach((java.util.Map.Entry<String, Integer> entry) -> matchedIntents.add(findIntentByName(entry.getKey())));

        return matchedIntents;
    }

    /**
     * 根据意图名称查找意图定义
     */
    private SkillIntent findIntentByName(String name) {
        return DEFINED_INTENTS.stream()
            .filter(intent -> intent.getName().equals(name))
            .findFirst()
            .orElse(DEFINED_INTENTS.get(DEFINED_INTENTS.size() - 1)); // 默认返回最后一个（通用咨询）
    }

    /**
     * 获取所有预定义的意图
     */
    public List<SkillIntent> getAllIntents() {
        return new ArrayList<>(DEFINED_INTENTS);
    }
}
