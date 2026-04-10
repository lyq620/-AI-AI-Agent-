package com.fox.aiagent.skills;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@SkillComponent(
    name = "date_budget",
    description = "智能拆分约会预算，根据约会类型和预算范围提供详细的费用分配建议"
)
// ✅ 修复：类名 = 文件名
@Component
public class DateBudgetSkill implements Skill {

    // ✅ 修复：添加日志对象（解决 log 找不到）
    private static final Logger log = LoggerFactory.getLogger(DateBudgetSkill.class);

    private final ChatClient chatClient;

    @Autowired
    public DateBudgetSkill(ChatModel dashscopeChatModel) {
        this.chatClient = ChatClient.create(dashscopeChatModel);
        log.info("【DateBudgetSkill】约会预算技能初始化完成");
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
        log.info("==================================================");
        log.info("【DateBudgetSkill】预算拆分技能 已被调用！");
        log.info("【DateBudgetSkill】接收参数：{}", parameters);
        log.info("==================================================");

        String totalBudget = (String) parameters.get("total_budget");
        String dateType = (String) parameters.get("date_type");
        String location = (String) parameters.get("location");

        if (totalBudget == null || totalBudget.trim().isEmpty()) {
            log.error("【DateBudgetSkill】参数校验失败：total_budget 参数不能为空");
            throw new SkillException("Total budget parameter is required");
        }

        try {
            log.info("【DateBudgetSkill】开始生成预算分配方案，总预算：{}", totalBudget);

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

            ChatResponse response = chatClient
                .prompt()
                .user(prompt.toString())
                .call()
                .chatResponse();

            String result = response.getResult().getOutput().getText();
            log.info("【DateBudgetSkill】预算方案生成完成，执行成功");
            return result;

        } catch (Exception e) {
            log.error("【DateBudgetSkill】执行异常：{}", e.getMessage(), e);
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