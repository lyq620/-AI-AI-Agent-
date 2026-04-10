package com.fox.aiagent.skills;

import java.util.Map;

/**
 * 技能接口，定义所有技能必须实现的方法
 */
public interface Skill {

    /**
     * 获取技能名称
     * @return 技能名称
     */
    String getName();

    /**
     * 获取技能描述
     * @return 技能描述
     */
    String getDescription();

    /**
     * 执行技能
     * @param parameters 参数映射
     * @return 执行结果
     */
    String execute(Map<String, Object> parameters);

    /**
     * 获取技能参数定义
     * @return 参数定义
     */
    Map<String, String> getParameterDefinitions();

    /**
     * 检查技能是否可用
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
}