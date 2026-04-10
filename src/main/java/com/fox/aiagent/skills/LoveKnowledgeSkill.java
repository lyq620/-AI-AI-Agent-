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

import com.fox.aiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.fox.aiagent.rag.QueryRewriter;

/**
 * 恋爱知识问答技能
 * 基于RAG知识库提供恋爱相关的知识问答服务
 */
@SkillComponent(
    name = "love_knowledge",
    description = "提供恋爱知识问答服务，基于RAG知识库回答用户的恋爱相关问题"
)
@Component
public class LoveKnowledgeSkill implements Skill {

    // ====================== 1. 新增日志对象 ======================
    private static final Logger logger = LoggerFactory.getLogger(LoveKnowledgeSkill.class);

    private final ChatClient chatClient;
    private final QueryRewriter queryRewriter;
    private final LoveAppRagCustomAdvisorFactory ragAdvisorFactory;

    @Autowired
    public LoveKnowledgeSkill(ChatModel dashscopeChatModel,
                           QueryRewriter queryRewriter,
                           LoveAppRagCustomAdvisorFactory ragAdvisorFactory) {
        this.chatClient = ChatClient.create(dashscopeChatModel);
        this.queryRewriter = queryRewriter;
        this.ragAdvisorFactory = ragAdvisorFactory;
        // 构造函数日志（验证Bean初始化）
        logger.info("【LoveKnowledgeSkill】恋爱知识技能初始化完成");
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
        // ====================== 2. 技能被调用核心日志 ======================
        logger.info("==================================================");
        logger.info("【LoveKnowledgeSkill】技能 已被调用！");
        logger.info("【LoveKnowledgeSkill】接收参数：{}", parameters);
        logger.info("==================================================");

        // 获取查询参数
        String query = (String) parameters.get("query");
        if (query == null || query.trim().isEmpty()) {
            logger.error("【LoveKnowledgeSkill】参数校验失败：query 参数不能为空");
            throw new SkillException("Query parameter is required");
        }

        try {
            logger.info("【LoveKnowledgeSkill】开始处理用户查询：{}", query);

            // 查询重写
            String rewrittenQuery = queryRewriter.doQueryRewrite(query);
            logger.info("【LoveKnowledgeSkill】查询重写完成：{}", rewrittenQuery);

            // 使用RAG知识库进行问答
            logger.info("【LoveKnowledgeSkill】调用RAG知识库进行问答");
            ChatResponse response = chatClient
                .prompt()
                .user(rewrittenQuery)
                .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(null, "恋爱知识"))
                .call()
                .chatResponse();

            String result = response.getResult().getOutput().getText();
            // 执行成功日志
            logger.info("【LoveKnowledgeSkill】执行完成，返回结果：{}", result);
            return result;

        } catch (Exception e) {
            logger.error("【LoveKnowledgeSkill】执行异常：{}", e.getMessage(), e);
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