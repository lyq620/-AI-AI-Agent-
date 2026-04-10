package com.fox.aiagent.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 技能注册器，自动注册所有标记为 @SkillComponent 的技能
 */
@Component
public class SkillRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(SkillRegistrar.class);

    private final SkillManager skillManager;
    private final ListableBeanFactory beanFactory;

    @Autowired
    public SkillRegistrar(SkillManager skillManager, ListableBeanFactory beanFactory) {
        this.skillManager = skillManager;
        this.beanFactory = beanFactory;
    }

    /**
     * 初始化时自动注册所有技能
     * 通过Spring的BeanFactory自动发现所有实现了Skill接口的Bean
     */
    @PostConstruct
    public void registerSkills() {
        logger.info("【SkillRegistrar】开始自动注册技能...");

        // 获取所有实现了 Skill 接口的 Bean（包括 @Component 和 @SkillComponent 注解的类）
        final var skillBeans = beanFactory.getBeansOfType(Skill.class);
        logger.info("【SkillRegistrar】发现 {} 个技能 Bean", skillBeans.size());

        skillBeans.forEach((beanName, skill) -> {
            try {
                // 检查技能是否标记了 @SkillComponent 注解
                Class<?> skillClass = skill.getClass();
                boolean hasSkillAnnotation = skillClass.isAnnotationPresent(SkillComponent.class);

                if (hasSkillAnnotation) {
                    String skillName = skill.getName();
                    logger.info("【SkillRegistrar】注册技能: {} (Bean: {})", skillName, beanName);
                    skillManager.registerSkill(skill);
                } else {
                    logger.warn("【SkillRegistrar】跳过未标记 @SkillComponent 的 Bean: {} - {}", beanName, skillClass.getName());
                }
            } catch (Exception e) {
                logger.error("【SkillRegistrar】注册技能失败: {} - {}", beanName, e.getMessage(), e);
            }
        });

        logger.info("【SkillRegistrar】技能注册完成，当前已注册 {} 个技能", skillManager.getAllSkills().size());
    }

    /**
     * 注册单个技能
     * @param skill 技能实例
     */
    public void register(Skill skill) {
        skillManager.registerSkill(skill);
    }

    /**
     * 批量注册技能
     * @param skills 技能列表
     */
    public void registerAll(List<Skill> skills) {
        skills.forEach(this::register);
    }
}