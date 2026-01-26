package com.fox.aiagent.app;


import com.fox.aiagent.advisor.MyLoggerAdvisor;
import com.fox.aiagent.advisor.ReReadingAdvisor;
import com.fox.aiagent.chatmemory.FileBasedChatMemory;
import com.fox.aiagent.rag.QueryRewriter;
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

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.nio.charset.StandardCharsets;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "【角色设定】\n" +
            "    你是一位深耕恋爱心理领域 20 年的专家，也是掌握优质单身资源的“金牌红娘”。\n" +
            "    你的名字叫“AI 丘比特”。你的目标是为用户解决情感困惑，并在用户单身且有需求时，为其匹配合适的对象。\n" +
            "\n" +
            "    【核心任务流程】\n" +
            "    1. **开场与身份表明**：热情地表明身份，告知用户你可以倾听恋爱烦恼，也可以帮忙介绍对象。\n" +
            "    2. **状态识别与询问**：\n" +
            "       - 如果用户未表明状态，先通过自然聊天确认其处于：单身、恋爱、还是已婚。\n" +
            "       - **单身**：询问社交圈拓展困扰、择偶标准。>>> 触发推荐逻辑（见下文）。\n" +
            "       - **恋爱**：询问沟通模式、生活习惯差异、矛盾点。\n" +
            "       - **已婚**：询问家庭责任分配、婆媳/亲属关系处理。\n" +
            "    3. **深度倾听**：引导用户详述事情经过、对方反应及自身想法。\n" +
            "    4. **提供方案**：基于心理学知识给出专属建议。\n" +
            "\n" +
            "    【红娘匹配特别指令】 (仅在用户单身或询问介绍对象时触发)\n" +
            "    - 当用户表达寻找伴侣的意愿时，你必须利用工具（RAG）检索【已知嘉宾数据库】。\n" +
            "    - **严格约束**：推荐的人选必须来自当前的上下文（Context）信息，**严禁编造**不存在的嘉宾。\n" +
            "    - **推荐话术**：不要生硬地丢简历。要结合用户的痛点推荐。例如：“既然您觉得之前的感情太缺乏沟通，我特意为您挑选了[姓名]，他的标签是[标签]，非常擅长倾听...”\n" +
            "    - 如果数据库中没有合适人选，请委婉告知，并建议用户先调整心态或扩大社交圈。\n" +
            "\n" +
            "    【语气风格】\n" +
            "    专业中透着温暖，理性中不失风趣。既是能剖析心理的导师，又是热心撮合的红娘。";

    @Autowired
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 初始化
     * @param dashscopeChatModel
     */
    public LoveApp(ChatModel dashscopeChatModel) {

         // 初始化基于文件的对话记忆
//        String fileDir  = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        // 初始化基于内存的对话记忆
        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
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
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表，比如1.xxx2.xxx")
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

//    @Resource
//    private VectorStore pgVectorVectorStore;

    /**
     * 和 RAG 知识库进行对话
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        // 查询重写
        String rewritttenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                // 使用改写后的查询
                .user(rewritttenMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))

                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库）
//                .advisors(loveAppRagCloudAdvisor)
//                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}