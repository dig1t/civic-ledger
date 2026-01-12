package com.civicledger.audit;

import com.civicledger.entity.AuditLog.ActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark service methods for automatic audit logging.
 * When applied, the AuditAspect will intercept calls and log them.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * The type of action being performed.
     */
    ActionType action();

    /**
     * The type of resource being accessed (e.g., "DOCUMENT", "USER").
     */
    String resourceType() default "";

    /**
     * SpEL expression to extract the resource ID from method arguments.
     * Example: "#documentId" or "#request.id"
     */
    String resourceIdExpression() default "";

    /**
     * Whether to include method arguments in the audit details.
     * Be careful with sensitive data.
     */
    boolean includeArgs() default false;

    /**
     * Whether to include the return value in the audit details.
     * Be careful with sensitive data.
     */
    boolean includeResult() default false;
}
