package com.fox.aiagent.skills;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 约会预算智能拆分技能
 * 根据约会类型和预算范围智能拆分各项费用
 */
@SkillComponent(
    name = "date_budget",
    description = "智能拆分约会预算，根据约会类型和预算范围提供详细的费用分配建议"
)
public class DateBudgetSkill implements Skill {

    private final ChatClient chatClient;

    @Autowired
    public DateBudgetSkill(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String getName() {
        return "date_budget";
    }

    @Override
    public String getDescription() {
        return "智能拆分约会预算，根据约会类型和预算范围提供详细的费用分配建议";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        // 获取参数
        String totalBudget = (String) parameters.get("total_budget");
        String dateType = (String) parameters.get("date_type");
        String location = (String) parameters.get("location");

        if (totalBudget == null || totalBudget.trim().isEmpty()) {
            throw new SkillException("Total budget parameter is required");
        }

        try {
            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请为用户制定约会预算分配方案。");
            prompt.append("总预算：").append(totalBudget).append("元。");

            if (dateType != null && !dateType.trim().isEmpty()) {
                prompt.append("约会类型：").append(dateType).append("。");
            }

            if (location != null && !location.trim().isEmpty()) {
                prompt.append("地点：").append(location).append("。");
            }

            prompt.append("请提供详细的预算分配建议，包括：");
            prompt.append("1. 餐饮费用（包括主餐、饮品、甜点等）");
            prompt.append("2. 娱乐活动费用（电影、游戏、演出等）");
            prompt.append("3. 交通费用（打车、公共交通等）");
            prompt.append("4. 礼物或小费（可选）");
            prompt.append("5. 应急备用金");
            prompt.append("6. 各项费用的具体金额和占比");
            prompt.append("7. 节省预算的建议");

            // 调用AI生成预算分配方案
            ChatResponse response = chatClient
                .prompt()
                .user(prompt.toString())
                .call()
                .chatResponse();

            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            throw new SkillException("Failed to generate budget plan: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getParameterDefinitions() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("total_budget", "总预算金额（必需，单位：元）");
        parameters.put("date_type", "约会类型（可选，如浪漫晚餐、户外活动、电影约会等）");
        parameters.put("location", "约会地点（可选，如城市或具体区域）");
        return parameters;
    }
}