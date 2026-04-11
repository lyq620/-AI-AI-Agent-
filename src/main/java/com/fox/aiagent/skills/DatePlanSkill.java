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
    name = "date_plan",
    description = "生成个性化的约会计划，根据用户偏好和需求定制约会方案"
)
// ✅ 修复：类名 = 文件名
@Component
public class DatePlanSkill implements Skill {

    // ✅ 修复：添加日志对象（解决 log 找不到）
    private static final Logger log = LoggerFactory.getLogger(DatePlanSkill.class);

    private final ChatClient chatClient;

    @Autowired
    public DatePlanSkill(ChatModel dashscopeChatModel) {
        this.chatClient = ChatClient.create(dashscopeChatModel);
        log.info("【DatePlanSkill】约会规划技能初始化完成");
    }

    @Override
    public String getName() {
        return "date_plan";
    }

    @Override
    public String getDescription() {
        return "生成个性化的约会计划，根据用户偏好和需求定制约会方案";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        log.info("==================================================");
        log.info("【DatePlanSkill】约会规划技能 已被调用！");
        log.info("【DatePlanSkill】接收参数：{}", parameters);
        log.info("==================================================");

        String userPreferences = (String) parameters.get("preferences");
        String budget = (String) parameters.get("budget");
        String location = (String) parameters.get("location");
        String dateType = (String) parameters.get("date_type");

        if (userPreferences == null || userPreferences.trim().isEmpty()) {
            log.error("【DatePlanSkill】参数校验失败：preferences 参数不能为空");
            throw new SkillException("Preferences parameter is required");
        }

        try {
            log.info("【DatePlanSkill】开始生成约会计划，用户偏好：{}", userPreferences);

            StringBuilder prompt = new StringBuilder();
            prompt.append("请为用户生成一个个性化的约会计划。");
            prompt.append("用户偏好：").append(userPreferences).append("。");

            if (budget != null && !budget.trim().isEmpty()) {
                prompt.append("预算范围：").append(budget).append("。");
            }

            if (location != null && !location.trim().isEmpty()) {
                prompt.append("地点偏好：").append(location).append("。");
            }

            if (dateType != null && !dateType.trim().isEmpty()) {
                prompt.append("约会类型：").append(dateType).append("。");
            }

            prompt.append("请提供详细的约会计划，包括：");
            prompt.append("1. 约会主题和创意");
            prompt.append("2. 具体活动安排");
            prompt.append("3. 时间安排建议");
            prompt.append("4. 预算分配建议");
            prompt.append("5. 注意事项和温馨提示");

            ChatResponse response = chatClient
                .prompt()
                .user(prompt.toString())
                .call()
                .chatResponse();

            String result = response.getResult().getOutput().getText();
            log.info("【DatePlanSkill】约会计划生成完成，执行成功");
            return result;

        } catch (Exception e) {
            log.error("【DatePlanSkill】执行异常：{}", e.getMessage(), e);
            throw new SkillException("Failed to generate date plan: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getParameterDefinitions() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("preferences", "用户偏好和需求（必需）");
        parameters.put("budget", "预算范围（可选）");
        parameters.put("location", "地点偏好（可选）");
        parameters.put("date_type", "约会类型（可选，如浪漫、休闲、户外等）");
        return parameters;
    }
}