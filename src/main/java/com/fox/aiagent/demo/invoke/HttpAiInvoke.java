package com.fox.aiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

public class HttpAiInvoke {

        // API 配置
        private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
        private static final String API_KEY = System.getenv("API-KEY"); // 替换为你的 API Key

        public static void main(String[] args) {
            // 方法1: 使用 JSONObject 构建请求体
            callApiMethod1();

        }

        /**
         * 方法1: 使用 Hutool 的 JSONObject 构建请求
         */
        public static void callApiMethod1() {
            // 构建 messages 数组
            JSONArray messages = new JSONArray();

            JSONObject systemMsg = new JSONObject();
            systemMsg.set("role", "system");
            systemMsg.set("content", "You are a helpful assistant.");
            messages.add(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", "你是谁？");
            messages.add(userMsg);

            // 构建 input 对象
            JSONObject input = new JSONObject();
            input.set("messages", messages);

            // 构建 parameters 对象
            JSONObject parameters = new JSONObject();
            parameters.set("result_format", "message");

            // 构建完整的请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", "qwen-plus");
            requestBody.set("input", input);
            requestBody.set("parameters", parameters);

            // 发送 HTTP 请求
            HttpResponse response = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .execute();

            // 处理响应
            if (response.isOk()) {
                String responseBody = response.body();
                System.out.println("响应内容: " + responseBody);

                // 解析 JSON 响应
                JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
                System.out.println("解析后的响应: " + jsonResponse);
            } else {
                System.out.println("请求失败，状态码: " + response.getStatus());
                System.out.println("错误信息: " + response.body());
            }
        }


}
