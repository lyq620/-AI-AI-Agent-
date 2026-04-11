# 技能测试指南

## 1. 启动测试

### 使用 Maven 运行所有测试
```bash
mvn test
```

### 运行特定测试类
```bash
mvn test -Dtest=SkillTests
```

```bash
mvn test -Dtest=SkillIntegrationTest
```

### 运行单个测试方法
```bash
mvn test -Dtest=SkillTests#testLoveKnowledgeSkill
```

## 2. 技能测试用例

### 恋爱知识问答技能测试
```java
@Test
void testLoveKnowledgeSkill() {
    Map<String, Object> params = new HashMap<>();
    params.put("query", "如何让另一半更爱我");
    String result = loveKnowledgeSkill.execute(params);
    assertNotNull(result);
}
```

### 约会计划技能测试
```java
@Test
void testDatePlanSkill() {
    Map<String, Object> params = new HashMap<>();
    params.put("preferences", "喜欢浪漫晚餐，喜欢看电影");
    params.put("budget", "500元");
    params.put("location", "上海");
    params.put("date_type", "浪漫约会");
    String result = datePlanSkill.execute(params);
    assertNotNull(result);
}
```

### 约会预算技能测试
```java
@Test
void testDateBudgetSkill() {
    Map<String, Object> params = new HashMap<>();
    params.put("total_budget", "1000");
    params.put("date_type", "浪漫晚餐");
    params.put("location", "北京");
    String result = dateBudgetSkill.execute(params);
    assertNotNull(result);
}
```

### 图片查询技能测试
```java
@Test
void testImageSearchSkill() {
    Map<String, Object> params = new HashMap<>();
    params.put("query", "星空情侣壁纸");
    params.put("image_type", "照片");
    params.put("style", "浪漫");
    String result = imageSearchSkill.execute(params);
    assertNotNull(result);
}
```

## 3. 集成测试

### 技能管理器测试
```java
@Test
void testSkillRegistration() {
    assertTrue(skillManager.hasSkill("love_knowledge"));
    assertTrue(skillManager.hasSkill("date_plan"));
    assertTrue(skillManager.hasSkill("date_budget"));
}

@Test
void testLoveKnowledgeSkillExecution() {
    Map<String, Object> params = new HashMap<>();
    params.put("query", "如何让另一半更爱我");
    String result = skillManager.executeSkill("love_knowledge", params);
    assertNotNull(result);
}
```

## 4. 常见问题解决

### 依赖问题
如果遇到 `ImageClient cannot be resolved` 错误，请确保：
1. 已在 `pom.xml` 中添加 Spring AI Image 依赖
2. 已运行 `mvn clean install` 更新依赖

### 构造函数注入问题
如果遇到构造函数注入错误，请检查：
1. 确保所有必需的依赖都已正确注入
2. 检查 `SkillsAutoConfiguration.java` 中的注册代码

### 测试失败
如果测试失败，请检查：
1. 确保测试环境已正确配置
2. 检查模拟对象是否正确设置
3. 确保技能参数正确传递

## 5. 测试覆盖率

当前测试覆盖了以下技能：
- ✅ LoveKnowledgeSkill (恋爱知识问答)
- ✅ DatePlanSkill (约会计划)
- ✅ DateBudgetSkill (约会预算)
- ✅ ImageSearchSkill (图片查询)

## 6. 扩展测试

如需添加新的测试用例，请：
1. 在 `SkillTests.java` 中添加新的测试方法
2. 在 `SkillIntegrationTest.java` 中添加集成测试
3. 确保测试覆盖所有边界情况

## 7. 运行测试报告

生成测试报告：
```bash
mvn surefire-report:report
```

查看测试覆盖率：
```bash
mvn jacoco:report
```