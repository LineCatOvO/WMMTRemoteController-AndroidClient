package com.linecat.wmmtcontroller.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记稳定的API
 * 这些API在2年内不会破坏，提供长期稳定性保证
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
public @interface Stable {
    /**
     * API稳定版本
     */
    String version() default "1.0.0";
    
    /**
     * API稳定日期
     */
    String since() default "2026-01-21";
}