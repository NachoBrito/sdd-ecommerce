package es.nachobrito.sdd.ecommerce.application.aop;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Micronaut AOP Interceptor applying cross-cutting structured logging and correlation context.
 */
@Singleton
@InterceptorBean(StructuredLog.class)
public class LoggingInterceptor implements MethodInterceptor<Object, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Object intercept(final MethodInvocationContext<Object, Object> context) {
        final String methodName = context.getMethodName();
        final String className = context.getDeclaringType().getSimpleName();
        final String previousTraceId = MDC.get("traceId");

        try {
            if (previousTraceId == null) {
                MDC.put("traceId", UUID.randomUUID().toString());
            }
            MDC.put("eventType", className + "." + methodName);

            LOG.debug("Entering {}.{}", className, methodName);
            final long start = System.currentTimeMillis();

            final Object result = context.proceed();

            final long durationMs = System.currentTimeMillis() - start;
            LOG.info("Completed {}.{} in {} ms", className, methodName, durationMs);
            return result;
        } catch (Exception ex) {
            LOG.error("Failed {}.{}: {}", className, methodName, ex.getMessage(), ex);
            throw ex;
        } finally {
            if (previousTraceId == null) {
                MDC.remove("traceId");
            }
            MDC.remove("eventType");
        }
    }
}
