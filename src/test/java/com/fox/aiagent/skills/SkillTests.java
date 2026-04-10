package com.fox.aiagent.skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.image.ImageResponse;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class SkillTests {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ObjectMapper objectMapper;

    private LoveKnowledgeSkill loveKnowledgeSkill;
    private DatePlanSkill datePlanSkill;
    private DateBudgetSkill dateBudgetSkill;

    @Test
    void testLoveKnowledgeSkill() {
        // 测试正常执行
        Map<String, Object> params = new HashMap<>();
        params.put("query", "如何让另一半更爱我");

        String result = loveKnowledgeSkill.execute(params);
        assertNotNull(result);
    }

    @Test
    void testLoveKnowledgeSkillMissingQuery() {
        // 测试缺少必需参数
        Map<String, Object> params = new HashMap<>();

        assertThrows(SkillException.class, () -> {
            loveKnowledgeSkill.execute(params);
        });
    }

    @Test
    void testDatePlanSkill() {
        // 测试正常执行
        Map<String, Object> params = new HashMap<>();
        params.put("preferences", "喜欢浪漫晚餐，喜欢看电影");
        params.put("budget", "500元");
        params.put("location", "上海");
        params.put("date_type", "浪漫约会");

        String result = datePlanSkill.execute(params);
        assertNotNull(result);
    }

    @Test
    void testDatePlanSkillMissingPreferences() {
        // 测试缺少必需参数
        Map<String, Object> params = new HashMap<>();
        params.put("budget", "500元");

        assertThrows(SkillException.class, () -> {
            datePlanSkill.execute(params);
        });
    }

    @Test
    void testDateBudgetSkill() {
        // 测试正常执行
        Map<String, Object> params = new HashMap<>();
        params.put("total_budget", "1000");
        params.put("date_type", "浪漫晚餐");
        params.put("location", "北京");

        String result = dateBudgetSkill.execute(params);
        assertNotNull(result);
    }

    @Test
    void testDateBudgetSkillMissingBudget() {
        // 测试缺少必需参数
        Map<String, Object> params = new HashMap<>();
        params.put("date_type", "浪漫晚餐");

        assertThrows(SkillException.class, () -> {
            dateBudgetSkill.execute(params);
        });
    }


    @Test
    void testParameterDefinitions() {
        // 测试参数定义
        Map<String, String> loveKnowledgeParams = loveKnowledgeSkill.getParameterDefinitions();
        assertEquals(1, loveKnowledgeParams.size());
        assertEquals("用户的问题或查询内容", loveKnowledgeParams.get("query"));

        Map<String, String> datePlanParams = datePlanSkill.getParameterDefinitions();
        assertEquals(4, datePlanParams.size());
        assertEquals("用户偏好和需求（必需）", datePlanParams.get("preferences"));

        Map<String, String> dateBudgetParams = dateBudgetSkill.getParameterDefinitions();
        assertEquals(3, dateBudgetParams.size());
        assertEquals("总预算金额（必需，单位：元）", dateBudgetParams.get("total_budget"));

    }
}