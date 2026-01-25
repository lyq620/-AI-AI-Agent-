package com.fox.aiagent.app;


import com.fox.aiagent.advisor.MyLoggerAdvisor;
import com.fox.aiagent.advisor.ReReadingAdvisor;
import com.fox.aiagent.chatmemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    // 1. 注入资源文件
    @Value("classpath:prompts/love-system-prompt.txt")
    private Resource systemPromptResource;
    // 2. 读取文件内容转换为 String
    String systemPromptText = systemPromptResource.toString();
    @Autowired
    private Advisor loveAppRagCloudAdvisor;

    /**
     * 初始化
     * @param dashscopeChatModel
     */
    public LoveApp(ChatModel dashscopeChatModel) {

        //        // 初始化基于文件的对话记忆
//        String fileDir  = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        // 初始化基于内存的对话记忆
        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPromptText)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启
//                        new ReReadingAdvisor() // 弊端：用户消息输入两遍，token翻倍
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)) // 0 的话就忘记了姓名
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        // 解决 java: 找不到符号 符号: 变量 log：：：确保启用了Annotation Processors 中的 Obtain processors...
        log.info("AI: {}", content);
        return content;
    }

    /**
     * 恋爱报告结构体,记录类（record）record语法用于创建不可变的数据载体类
     * @param title
     * @param suggestions
     */
    record LoveReport(String title, List<String> suggestions) {

    }

    /**
     * AI 恋爱报告功能（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(systemPromptText + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表，比如1.xxx2.xxx")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)) // 0 的话就忘记了姓名
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    // AI 恋爱知识库问答
    @Resource
    private VectorStore loveAppVectorStore;

    /**
     * 和 RAG 知识库进行对话
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))

                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
//                // 应用 RAG 知识库问答
//                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库）
                .advisors(loveAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}