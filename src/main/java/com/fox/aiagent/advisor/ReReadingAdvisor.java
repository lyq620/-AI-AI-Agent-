package com.fox.aiagent.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义 Re2 Advisor
 * 可提高大型语言模型的推理能力
 */
public class ReReadingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    /**
     * 执行请求前，改写 Prompt
     *
     * @param advisedRequest
     * @return
     */
    private AdvisedRequest before(AdvisedRequest advisedRequest) {

        Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        advisedUserParams.put("re2_input_query", advisedRequest.userText());

        return AdvisedRequest.from(advisedRequest)
                .userText("""
                        {re2_input_query}
                        Read the question again: {re2_input_query}
                        """)
                .userParams(advisedUserParams)
                .build();
        // 2026-01-24T14:46:57.194+08:00  INFO 27736 --- [ai-agent] [           main] c.fox.aiagent.advisor.MyLoggerAdvisor    :
        // AI Request: {re2_input_query} Read the question again: {re2_input_query}
        // 不管你怎么改 order，Logger 都不可能看到 {re2_input_query} 被替换后的值
        // 原因不是顺序问题，而是：
        // 👉 re2 变量替换根本不发生在 Advisor 阶段，Advisor 操作的是 PromptTemplate，而不是最终 Prompt
        // 1️⃣ 所有 Advisors.before() 执行完
        // 2️⃣ 所有 Advisors.around() 链执行完
        // ----------------------------------
        // 3️⃣ PromptTemplate.render()   👈 re2 在这里！
        // ----------------------------------
        // 4️⃣ ChatModel.call()
        // 5️⃣ after / stream 聚合
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        return chain.nextAroundCall(this.before(advisedRequest));
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(this.before(advisedRequest));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
