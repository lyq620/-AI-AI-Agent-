package com.fox.aiagent.skills;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 技能工具回调提供者，将 Skills 集成到 MCP 架构
 */
@Slf4j
@Component
@Primary
public class SkillToolCallbackProvider implements ToolCallbackProvider {

    private final SkillManager skillManager;
    private final ObjectMapper objectMapper;

    @Autowired
    public SkillToolCallbackProvider(SkillManager skillManager) {
        this.skillManager = skillManager;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return skillManager.getAllSkills().entrySet().stream()
            .map(entry -> new SkillToolCallback(entry.getKey(), entry.getValue()))
            .toArray(ToolCallback[]::new);
    }

    /**
     * 技能工具回调实现
     */
    private class SkillToolCallback implements ToolCallback {
        private final String skillName;
        private final Skill skill;

        public SkillToolCallback(String skillName, Skill skill) {
            this.skillName = skillName;
            this.skill = skill;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            // 构建 JSON Schema 格式的参数定义
            String inputSchema = buildInputSchema();
            
            return DefaultToolDefinition.builder()
                .name(skillName)
                .description(skill.getDescription())
                .inputSchema(inputSchema)
                .build();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            // 默认实现即可，或自定义返回元数据
            return ToolMetadata.builder().build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, org.springframework.ai.chat.model.ToolContext toolContext) {
            try {
                log.info("Executing skill: {} with input: {}", skillName, toolInput);
                
                // 解析 JSON 输入为 Map
                Map<String, Object> parameters = objectMapper.readValue(
                    toolInput, 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
                
                // 执行技能
                Object result = skill.execute(parameters);
                
                // 将结果转换为 JSON 字符串返回
                String resultJson = objectMapper.writeValueAsString(result);
                log.info("Skill execution completed: {} Result: {}", skillName, resultJson);
                
                return resultJson;
                
            } catch (Exception e) {
                log.error("Failed to execute skill {}: {}", skillName, e.getMessage(), e);
                return buildErrorResponse(e.getMessage());
            }
        }

        /**
         * 构建 JSON Schema 格式的输入参数定义
         */
        private String buildInputSchema() {
            try {
                Map<String, Object> schema = new HashMap<>();
                schema.put("type", "object");
                
                Map<String, Object> properties = new HashMap<>();
                Map<String, Object> required = new HashMap<>();
                List<String> requiredFields = new ArrayList<>();
                
                // 从技能参数定义构建 properties
                skill.getParameterDefinitions().forEach((name, description) -> {
                    Map<String, Object> property = new HashMap<>();
                    property.put("type", "string");
                    property.put("description", description);
                    properties.put(name, property);
                    
                    // 假设所有参数都是必需的
                    requiredFields.add(name);
                });
                
                schema.put("properties", properties);
                schema.put("required", requiredFields);
                
                return objectMapper.writeValueAsString(schema);
                
            } catch (Exception e) {
                log.error("Failed to build input schema for skill {}", skillName, e);
                return "{}";
            }
        }

        /**
         * 构建错误响应
         */
        private String buildErrorResponse(String errorMessage) {
            try {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", true);
                errorResponse.put("message", errorMessage);
                errorResponse.put("skill", skillName);
                return objectMapper.writeValueAsString(errorResponse);
            } catch (Exception e) {
                return "{\"error\": true, \"message\": \"Internal error\"}";
            }
        }
    }
}