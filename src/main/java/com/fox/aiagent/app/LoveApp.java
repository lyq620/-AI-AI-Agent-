package com.fox.aiagent.app;


import com.fox.aiagent.advisor.MyLoggerAdvisor;
import com.fox.aiagent.advisor.ProhibitedWordAdvisor;
import com.fox.aiagent.advisor.ReReadingAdvisor;
import com.fox.aiagent.chatmemory.FileBasedChatMemory;
import com.fox.aiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.fox.aiagent.rag.QueryRewriter;
import com.fox.aiagent.skills.SkillResult;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fox.aiagent.skills.Skill;
import com.fox.aiagent.skills.SkillIntentClassifier;
import com.fox.aiagent.skills.SkillManager;
import com.fox.aiagent.skills.SkillToolCallbackProvider;

// $env:JAVA_HOME = "D://tool//JDK//JDK21"
// $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
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
            "    专业中透着温暖，理性中不失风趣。既是能剖析心理的导师，又是热心撮合的红娘。" +
            "如果使用了工具，请标明来源。可以提供地图搜索和图片搜索等功能。";

    @Autowired
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private QueryRewriter queryRewriter;

    // Skills 系统
    private final SkillManager skillManager;
    private final SkillToolCallbackProvider skillToolCallbackProvider;

    /**
     * 初始化
     * @param dashscopeChatModel
     * @param skillManager 技能管理器
     * @param skillToolCallbackProvider 技能工具回调提供者
     */
    public LoveApp(ChatModel dashscopeChatModel,
                  SkillManager skillManager,
                  SkillToolCallbackProvider skillToolCallbackProvider) {

         // 初始化基于文件的对话记忆
//        String fileDir  = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        // 初始化基于内存的对话记忆
        ChatMemory chatMemory = new InMemoryChatMemory();
        this.skillManager = skillManager;
        this.skillToolCallbackProvider = skillToolCallbackProvider;
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor(),
                        // 违禁词检测 - 从文件读取违禁词
                        new ProhibitedWordAdvisor()
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
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
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
//    @Resource
//    private VectorStore loveAppVectorStore;
//
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
                //.advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库）
//                .advisors(loveAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
//                .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                        loveAppVectorStore, "已婚"
//                ))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

//    // AI 调用工具能力
//    @Resource
//    private ToolCallback[] allTools;
//
//    /**
//     * AI 恋爱报告功能（支持调用工具）
//     *
//     * @param message
//     * @param chatId
//     * @return
//     */
//    public String doChatWithTools(String message, String chatId) {
//        ChatResponse response = chatClient
//                .prompt()
//                .user(message)
//                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
//                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
//
//                .advisors(new MyLoggerAdvisor())
//                .tools(allTools)
//                .call()
//                .chatResponse();
//        String content = response.getResult().getOutput().getText();
//        log.info("content: {}", content);
//        return content;
//    }

    // AI 调用 MCP 服务
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 恋爱报告功能（调用 MCP 服务）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))

                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    /**
     * 使用 Skills 系统执行技能
     *
     * @param skillName 技能名称
     * @param parameters 参数
     * @param chatId 对话ID
     * @return 执行结果
     */
    public String executeSkill(String skillName, Map<String, Object> parameters, String chatId) {
        try {
            SkillResult<String> result = skillManager.executeSkill(skillName, parameters);
            if (result.isSuccess()) {
                return result.getData();
            } else {
                return "技能执行失败: " + result.getErrorMessage();
            }
        } catch (Exception e) {
            log.error("Failed to execute skill {}: {}", skillName, e.getMessage());
            return "抱歉，执行技能时发生错误: " + e.getMessage();
        }
    }

    /**
     * 获取所有可用技能
     *
     * @return 技能列表
     */
    public Map<String, Skill> getAllSkills() {
        return skillManager.getAllSkills();
    }

    /**
     * 检查技能是否存在
     *
     * @param skillName 技能名称
     * @return 是否存在
     */
    public boolean hasSkill(String skillName) {
        return skillManager.hasSkill(skillName);
    }

    static {
        System.setProperty("CHAT_MEMORY_CONVERSATION_ID_KEY", "conversation_id");
        System.setProperty("CHAT_MEMORY_RETRIEVE_SIZE_KEY", "retrieve_size");
    }

    // ======================== 【关键：让AI调用技能】 ========================
    @Resource
    private ToolCallback[] allTools;

    @Resource
    private SkillIntentClassifier skillIntentClassifier;

    /**
     * 使用动态技能选择的对话
     * 解决 Token 爆炸问题：先识别意图，再只加载相关的技能 schema
     *
     * @param message 用户消息
     * @param chatId 会话ID
     * @return AI 回复
     */
    public String doChatWithSkills(String message, String chatId) {
        // 1. 意图识别：判断用户需要哪个技能
        List<SkillIntentClassifier.SkillIntent> intents = skillIntentClassifier.classifyIntent(message);
        String primaryIntent = intents.isEmpty() ? "通用咨询" : intents.get(0).getName();

        log.info("【LoveApp】意图识别结果: {} | 用户消息: {}", primaryIntent, message);

        // 2. 根据意图获取相关的技能 ToolCallback（只加载相关的，减少 Token）
        ToolCallback[] relevantTools = skillManager.getToolCallbacksByIntent(primaryIntent);

        // 3. 如果没有匹配的技能，回退到普通对话
        if (relevantTools.length == 0) {
            log.warn("【LoveApp】没有匹配到相关技能，回退到普通对话模式");
            return doChat(message, chatId);
        }

        log.info("【LoveApp】加载了 {} 个相关技能: {}", relevantTools.length,
                Arrays.stream(relevantTools).map(t -> t.getToolDefinition().name()).collect(Collectors.toList()));

        // 4. 使用动态选择的技能进行对话
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)
                )
                .tools(relevantTools) // 🔥 只传相关的技能，减少 Token
                .call()
                .content();
    }
// ====================================================================
}