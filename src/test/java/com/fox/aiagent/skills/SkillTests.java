package com.fox.aiagent.skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SkillTests {

    // 🔥 核心修复：加上 @Autowired 自动注入Bean（不再是null）
    @Autowired
    private LoveKnowledgeSkill loveKnowledgeSkill;

    @Autowired
    private DatePlanSkill datePlanSkill;

    @Autowired
    private DateBudgetSkill dateBudgetSkill;

//    // 模拟Spring AI的客户端
//    @MockBean
//    private ChatClient chatClient;
//
//    @MockBean
//    private ObjectMapper objectMapper;

    @Test
    void testLoveKnowledgeSkill() {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "如何让另一半更爱我");
        String result = loveKnowledgeSkill.execute(params);
        assertNotNull(result);
    }

    @Test
    void testLoveKnowledgeSkillMissingQuery() {
        Map<String, Object> params = new HashMap<>();
        assertThrows(SkillException.class, () -> {
            loveKnowledgeSkill.execute(params);
        });
    }

    @Test
    void testDatePlanSkill() {
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
        Map<String, Object> params = new HashMap<>();
        params.put("budget", "500元");
        assertThrows(SkillException.class, () -> {
            datePlanSkill.execute(params);
        });
    }

    @Test
    void testDateBudgetSkill() {
        Map<String, Object> params = new HashMap<>();
        params.put("total_budget", "1000");
        params.put("date_type", "浪漫晚餐");
        params.put("location", "北京");
        String result = dateBudgetSkill.execute(params);
        assertNotNull(result);
    }

    @Test
    void testDateBudgetSkillMissingBudget() {
        Map<String, Object> params = new HashMap<>();
        params.put("date_type", "浪漫晚餐");
        assertThrows(SkillException.class, () -> {
            dateBudgetSkill.execute(params);
        });
    }

    @Test
    void testParameterDefinitions() {
        Map<String, String> loveKnowledgeParams = loveKnowledgeSkill.getParameterDefinitions();
        assertEquals(1, loveKnowledgeParams.size());

        Map<String, String> datePlanParams = datePlanSkill.getParameterDefinitions();
        assertEquals(4, datePlanParams.size());

        Map<String, String> dateBudgetParams = dateBudgetSkill.getParameterDefinitions();
        assertEquals(3, dateBudgetParams.size());
    }
}