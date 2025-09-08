package org.kasbench.globeco_trade_service.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AOP aspect to monitor query performance by intercepting repository method calls.
 * This approach avoids circular dependencies with Hibernate configuration.
 */
@Aspect
@Component
public class QueryPerformanceInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryPerformanceInterceptor.class);
    
    @Autowired
    private QueryPerformanceMonitor queryPerformanceMonitor;
    
    /**
     * Intercept all repository method calls to monitor query performance
     */
    @Around("execution(* org.kasbench.globeco_trade_service.repository..*(..))")
    public Object monitorRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String queryType = classifyRepositoryMethod(methodName);
        
        // Start timing
        queryPerformanceMonitor.startQueryTiming(className + "." + methodName, queryType);
        
        try {
            // Execute the actual repository method
            return joinPoint.proceed();
        } finally {
            // End timing
            queryPerformanceMonitor.endQueryTiming();
        }
    }
    
    /**
     * Classify repository method type based on method name
     */
    private String classifyRepositoryMethod(String methodName) {
        String lowerMethodName = methodName.toLowerCase();
        
        if (lowerMethodName.startsWith("find") || lowerMethodName.startsWith("get") || 
            lowerMethodName.startsWith("read") || lowerMethodName.startsWith("query") ||
            lowerMethodName.startsWith("search") || lowerMethodName.startsWith("count") ||
            lowerMethodName.startsWith("exists")) {
            return "select";
        } else if (lowerMethodName.startsWith("save") || lowerMethodName.startsWith("insert") ||
                   lowerMethodName.startsWith("create")) {
            return "insert";
        } else if (lowerMethodName.startsWith("update") || lowerMethodName.startsWith("modify")) {
            return "update";
        } else if (lowerMethodName.startsWith("delete") || lowerMethodName.startsWith("remove")) {
            return "delete";
        } else {
            return "other";
        }
    }
}