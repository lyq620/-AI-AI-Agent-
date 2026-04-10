package com.fox.aiagent.skills;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 技能组件注解，用于标记技能类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SkillComponent {
    /**
     * 技能名称（可选，如果不指定则使用类名）
     * @return 技能名称
     */
    String name() default "";

    /**
     * 技能描述
     * @return 技能描述
     */
    String description() default "";
}