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

    /**
     * 校验参数是否合法
     * 在执行前调用，如果参数不合法抛出 SkillValidationException
     *
     * @param parameters 参数映射
     * @throws SkillValidationException 参数不合法时抛出
     */
    default void validate(Map<String, Object> parameters) throws SkillValidationException {
        // 默认实现：校验所有标记为"必需"的参数
        Map<String, String> paramDefs = getParameterDefinitions();
        for (Map.Entry<String, String> entry : paramDefs.entrySet()) {
            String paramName = entry.getKey();
            String description = entry.getValue();

            // 如果参数标记为"必需"，则校验不能为空
            if (description.contains("必需") || description.contains("必须")) {
                Object value = parameters.get(paramName);
                if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                    throw new SkillValidationException(getName(), paramName, value,
                        "Required parameter is missing or empty");
                }
            }
        }
    }
}