package com.linecat.wmmtcontroller.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实验性API
 * 这些API可能随时变化，不提供长期稳定性保证
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
public @interface Experimental {
    /**
     * 实验性API的版本
     */
    String version() default "1.0.0";
    
    /**
     * 实验性API的引入日期
     */
    String since() default "2026-01-21";
    
    /**
     * 该API的预期状态
     */
    String status() default "experimental";
}