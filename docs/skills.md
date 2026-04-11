# AI 情感咨询 Skills 系统

## 概述

本项目实现了一个可插拔的 Skills 系统，用于扩展 AI 情感咨询和恋爱约会规划功能。该系统基于 Spring AI 和 MCP 架构，支持动态注册和管理各种技能，使 AI 能够提供更丰富的服务。

## 系统架构

### 核心组件

#### 1. Skill 接口
```java
public interface Skill {
    String getName();
    String getDescription();
    String execute(Map<String, Object> parameters);
    Map<String, String> getParameterDefinitions();
    default boolean isAvailable() { return true; }
}
```

**说明**：
- `getName()`: 获取技能名称
- `getDescription()`: 获取技能描述
- `execute()`: 执行技能逻辑
- `getParameterDefinitions()`: 定义技能参数
- `isAvailable()`: 检查技能是否可用（默认始终可用）

#### 2. SkillManager
技能管理器，负责：
- 技能的注册和注销
- 技能的查找和执行
- 技能可用性检查

#### 3. SkillRegistrar
技能注册器，负责自动发现和注册技能。

#### 4. SkillToolCallbackProvider
将 Skills 集成到 MCP 架构，使 AI 能够调用这些技能。

## 核心技能

### 1. LoveKnowledgeSkill - 恋爱知识问答

**功能**：基于 RAG 知识库提供恋爱相关的知识问答服务。

**参数**：
- `query`: 用户的问题或查询内容（必需）

**示例**：
```java
// 执行技能
Map<String, Object> parameters = new HashMap<>();
parameters.put("query", "如何处理恋爱中的沟通问题？");
String result = loveApp.executeSkill("love_knowledge", parameters, chatId);
```

**返回**：基于知识库的详细答案。

### 2. DatePlanSkill - 个性化约会计划生成

**功能**：根据用户偏好生成个性化的约会计划。

**参数**：
- `preferences`: 用户偏好和需求（必需）
- `budget`: 预算范围（可选）
- `location`: 地点偏好（可选）
- `date_type`: 约会类型（可选，如浪漫、休闲、户外等）

**示例**：
```java
// 执行技能
Map<String, Object> parameters = new HashMap<>();
parameters.put("preferences", "喜欢浪漫晚餐，预算中等");
parameters.put("budget", "500-800元");
parameters.put("location", "市中心");
parameters.put("date_type", "浪漫晚餐");
String result = loveApp.executeSkill("date_plan", parameters, chatId);
```

**返回**：详细的约会计划，包括主题、活动安排、时间建议等。

### 3. DateBudgetSkill - 约会预算智能拆分

**功能**：根据约会类型和预算范围智能拆分各项费用。

**参数**：
- `total_budget`: 总预算金额（必需，单位：元）
- `date_type`: 约会类型（可选，如浪漫晚餐、户外活动、电影约会等）
- `location`: 约会地点（可选，如城市或具体区域）

**示例**：
```java
// 执行技能
Map<String, Object> parameters = new HashMap<>();
parameters.put("total_budget", "800");
parameters.put("date_type", "浪漫晚餐");
parameters.put("location", "北京");
String result = loveApp.executeSkill("date_budget", parameters, chatId);
```

**返回**：详细的预算分配建议，包括餐饮、娱乐、交通等各项费用的具体金额和占比。

## 使用方式

### 1. 在 LoveApp 中使用 Skills

```java
// 获取所有可用技能
Map<String, Skill> skills = loveApp.getAllSkills();

// 检查特定技能是否存在
boolean hasLoveKnowledge = loveApp.hasSkill("love_knowledge");

// 执行特定技能
Map<String, Object> parameters = new HashMap<>();
parameters.put("query", "如何处理恋爱中的沟通问题？");
String result = loveApp.executeSkill("love_knowledge", parameters, chatId);
```

### 2. 在 MCP 对话中使用 Skills

Skills 已集成到 MCP 架构中，AI 可以自动调用这些技能：

```java
// 使用 MCP 服务，AI 会自动识别并调用合适的技能
String response = loveApp.doChatWithMcp("帮我制定一个浪漫的约会计划，预算500元", chatId);
```

## API 参考文档

### LoveApp 方法

#### `executeSkill(String skillName, Map<String, Object> parameters, String chatId)`

**描述**：执行指定的技能。

**参数**：
- `skillName`: 技能名称（如 "love_knowledge", "date_plan", "date_budget"）
- `parameters`: 技能参数映射
- `chatId`: 对话ID

**返回**：技能执行结果

#### `getAllSkills()`

**描述**：获取所有可用技能。

**返回**：技能名称到 Skill 对象的映射

#### `hasSkill(String skillName)`

**描述**：检查技能是否存在。

**参数**：
- `skillName`: 技能名称

**返回**：如果技能存在返回 true，否则返回 false

## 扩展指南

### 1. 创建新技能

要创建一个新的技能，需要：

1. 实现 `Skill` 接口
2. 添加 `@SkillComponent` 注解
3. 在 `SkillsAutoConfiguration` 中注册

**示例**：
```java
@SkillComponent(
    name = "new_skill",
    description = "新技能的描述"
)
public class NewSkill implements Skill {
    
    @Autowired
    private ChatClient chatClient;
    
    @Override
    public String getName() {
        return "new_skill";
    }
    
    @Override
    public String getDescription() {
        return "新技能的描述";
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        // 技能逻辑
        return "执行结果";
    }
    
    @Override
    public Map<String, String> getParameterDefinitions() {
        Map<String, String> params = new HashMap<>();
        params.put("param1", "参数1的描述");
        return params;
    }
}
```

### 2. 注册技能

在 `SkillsAutoConfiguration` 中注册新技能：

```java
@Configuration
public class SkillsAutoConfiguration {
    
    private final SkillRegistrar skillRegistrar;
    
    @Autowired
    public SkillsAutoConfiguration(SkillRegistrar skillRegistrar) {
        this.skillRegistrar = skillRegistrar;
    }
    
    public void registerSkills() {
        // 注册新技能
        skillRegistrar.register(new NewSkill(null));
    }
}
```

### 3. 使用依赖注入

确保在构造函数中注入必要的依赖：

```java
public NewSkill(ChatClient chatClient) {
    this.chatClient = chatClient;
}
```

## 常见问题

### Q: 如何调试技能执行？

A: 可以在技能的 `execute` 方法中添加日志：

```java
@Override
public String execute(Map<String, Object> parameters) {
    log.info("Executing skill with parameters: {}", parameters);
    // 技能逻辑
    return "结果";
}
```

### Q: 技能执行失败怎么办？

A: `executeSkill` 方法已经包含基本的错误处理，会返回错误信息。可以通过日志查看详细错误：

```java
try {
    String result = loveApp.executeSkill("skill_name", parameters, chatId);
} catch (Exception e) {
    log.error("Skill execution failed", e);
}
```

### Q: 如何添加更多的技能参数？

A: 在 `getParameterDefinitions()` 方法中添加参数定义：

```java
@Override
public Map<String, String> getParameterDefinitions() {
    Map<String, String> params = new HashMap<>();
    params.put("param1", "参数1的描述");
    params.put("param2", "参数2的描述");
    return params;
}
```

## 最佳实践

1. **参数验证**：在技能执行前验证必需参数
2. **错误处理**：提供有意义的错误信息
3. **日志记录**：记录技能执行的关键信息
4. **文档**：为每个技能提供清晰的文档
5. **测试**：编写单元测试确保技能正常工作

## 示例场景

### 场景1：用户询问恋爱知识

**用户输入**：`"如何处理恋爱中的沟通问题？"`

**AI 响应**：
```
我正在使用恋爱知识技能来回答您的问题...
[基于 RAG 知识库的详细答案]
```

### 场景2：用户请求约会计划

**用户输入**：`"帮我制定一个浪漫的约会计划，预算500元"`

**AI 响应**：
```
我正在为您生成约会计划...
[使用 DatePlanSkill 生成的详细计划]
```

### 场景3：用户询问预算分配

**用户输入**：`"帮我分配800元的约会预算"`

**AI 响应**：
```
我正在为您制定预算分配方案...
[使用 DateBudgetSkill 生成的详细预算分配]
```

## 总结

这个 Skills 系统为你的情感咨询 AI Agent 提供了强大的扩展能力，使 AI 能够提供更专业、更个性化的服务。通过简单的接口和自动注册机制，你可以轻松添加新的技能来满足不同的用户需求。