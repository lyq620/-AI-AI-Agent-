# Git 提交指南

## 1. Git 提交命令

### 基本提交
```bash
git add .
git commit -m "feat: 添加技能系统及相关测试"
```

### 带签名的提交
```bash
git commit -s -m "feat: 添加技能系统及相关测试"
```

### 分步提交
```bash
# 添加特定文件
git add src/main/java/com/fox/aiagent/skills/
git add src/test/java/com/fox/aiagent/skills/

# 提交
git commit -m "feat: 添加技能系统及相关测试"
```

## 2. 提交信息规范

### 常用提交类型
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构代码
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

### 示例提交信息
```bash
feat: 添加技能系统及相关测试

- 添加 LoveKnowledgeSkill 恋爱知识问答技能
- 添加 DatePlanSkill 个性化约会计划技能  
- 添加 DateBudgetSkill 约会预算智能拆分技能
- 添加 ImageSearchSkill 图片查询技能
- 添加 SkillTests 测试用例
- 添加 SkillIntegrationTest 集成测试
- 添加 TESTING_GUIDE.md 测试指南
- 添加 GIT_COMMIT_GUIDE.md Git提交指南
- 修复 SkillsAutoConfiguration 中的依赖问题
- 修复 ImageSearchSkill 中的依赖问题
```

### 简洁提交信息
```bash
feat: 添加技能系统及相关测试
```

## 3. 提交检查

### 检查当前状态
```bash
git status
```

### 查看差异
```bash
git diff
```

### 查看提交历史
```bash
git log --oneline -10
```

## 4. 推送代码

### 推送到远程仓库
```bash
git push origin appmod/java-upgrade-20260405104551
```

### 创建 Pull Request
```bash
gh pr create --title "feat: 添加技能系统及相关测试" --body "$(cat <<'EOF'
## Summary
添加完整的技能系统，包括：

1. 恋爱知识问答技能 (LoveKnowledgeSkill)
2. 个性化约会计划技能 (DatePlanSkill)  
3. 约会预算智能拆分技能 (DateBudgetSkill)
4. 图片查询技能 (ImageSearchSkill)

## 测试
- 添加了完整的测试用例验证技能功能
- 包含单元测试和集成测试
- 测试覆盖主要功能场景

## 依赖
- 添加了 Spring AI Image 依赖
- 修复了技能注册和依赖注入问题

## 文档
- 添加了测试指南
- 添加了 Git 提交指南

## 验证步骤
1. 运行 `mvn test` 验证所有测试通过
2. 验证技能注册和执行正常
3. 验证参数定义正确
EOF
)"
```

## 5. 版本回滚

### 回滚最近一次提交
```bash
git reset --hard HEAD~1
```

### 回滚到特定提交
```bash
git reset --hard <commit-hash>
```

### 回滚并保留更改
```bash
git reset --soft HEAD~1
```

### 撤销最近一次提交
```bash
git revert HEAD
```

### 撤销特定提交
```bash
git revert <commit-hash>
```

## 6. 分支管理

### 创建新分支
```bash
git checkout -b feature/skills-system
```

### 切换分支
```bash
git checkout appmod/java-upgrade-20260405104551
```

### 合并分支
```bash
git merge feature/skills-system
```

### 删除分支
```bash
git branch -d feature/skills-system
```

## 7. 常见问题

### 提交被拒绝
如果提交被拒绝，请：
1. 检查代码冲突
2. 运行测试确保所有测试通过
3. 确保提交信息符合规范

### 依赖问题
如果遇到依赖问题：
1. 运行 `mvn clean install`
2. 检查 `pom.xml` 中的依赖配置
3. 确保所有必需的依赖已添加

### 测试失败
如果测试失败：
1. 检查测试用例
2. 确保模拟对象正确设置
3. 检查技能参数传递

## 8. 最佳实践

- 每次提交只包含一个相关更改
- 提供清晰的提交信息
- 运行测试确保代码质量
- 定期推送代码到远程仓库
- 使用分支进行功能开发

## 9. 提交模板

```bash
<type>: <subject>

<body>

<footer>
```