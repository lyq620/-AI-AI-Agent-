package com.fox.aiagent.skills;

import com.fox.aiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.fox.aiagent.rag.QueryRewriter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 恋爱知识问答技能
 * 基于RAG知识库提供恋爱相关的知识问答服务
 */
@SkillComponent(
    name = "love_knowledge",
    description = "提供恋爱知识问答服务，基于RAG知识库回答用户的恋爱相关问题"
)
public class LoveKnowledgeSkill implements Skill {

    private final ChatClient chatClient;
    private final QueryRewriter queryRewriter;
    private final LoveAppRagCustomAdvisorFactory ragAdvisorFactory;

    @Autowired
    public LoveKnowledgeSkill(ChatClient chatClient,
                           QueryRewriter queryRewriter,
                           LoveAppRagCustomAdvisorFactory ragAdvisorFactory) {
        this.chatClient = chatClient;
        this.queryRewriter = queryRewriter;
        this.ragAdvisorFactory = ragAdvisorFactory;
    }

    @Override
    public String getName() {
        return "love_knowledge";
    }

    @Override
    public String getDescription() {
        return "提供恋爱知识问答服务，基于RAG知识库回答用户的恋爱相关问题";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        // 获取查询参数
        String query = (String) parameters.get("query");
        if (query == null || query.trim().isEmpty()) {
            throw new SkillException("Query parameter is required");
        }

        try {
            // 查询重写
            String rewrittenQuery = queryRewriter.doQueryRewrite(query);

            // 使用RAG知识库进行问答
            ChatResponse response = chatClient
                .prompt()
                .user(rewrittenQuery)
                .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(null, "恋爱知识"))
                .call()
                .chatResponse();

            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            throw new SkillException("Failed to execute love knowledge skill: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getParameterDefinitions() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "用户的问题或查询内容");
        return parameters;
    }
}