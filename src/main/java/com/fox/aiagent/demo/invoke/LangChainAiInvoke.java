package com.fox.aiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class LangChainAiInvoke {

    public static void main(String[] args) {
        ChatLanguageModel qwenModel = QwenChatModel.builder()
                .apiKey(System.getenv("API-KEY"))
                .modelName("qwen-max")
                .build();
        String answer = qwenModel.chat("我是fox,怎么学习Java？？");
        System.out.println(answer);
    }
}