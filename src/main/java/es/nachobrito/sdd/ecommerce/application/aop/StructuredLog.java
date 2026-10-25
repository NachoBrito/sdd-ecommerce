package es.nachobrito.sdd.ecommerce.application.aop;

import io.micronaut.aop.Around;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation enabling declarative structured logging via AOP interceptor.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Around
public @interface StructuredLog {

    /**
     * Optional custom operation name for logging.
     *
     * @return Operation name
     */
    String value() default "";
}
