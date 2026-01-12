package com.civicledger.audit;

import com.civicledger.entity.AuditLog.ActionType;
import com.civicledger.entity.AuditLog.AuditStatus;
import com.civicledger.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * AOP Aspect for automatic audit logging of service methods.
 * Intercepts methods annotated with @Auditable and logs the action.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        ActionType actionType = auditable.action();
        String resourceType = auditable.resourceType();
        String resourceId = extractResourceId(joinPoint, auditable);
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();

        String methodDetails = buildMethodDetails(joinPoint, auditable);

        try {
            Object result = joinPoint.proceed();

            // Log successful execution
            String resultDetails = auditable.includeResult() && result != null
                    ? methodDetails + " | Result: " + summarizeResult(result)
                    : methodDetails;

            auditService.log(actionType, resourceType, resourceId, AuditStatus.SUCCESS,
                    resultDetails, ipAddress, userAgent);

            return result;

        } catch (AccessDeniedException e) {
            // Log access denied
            auditService.log(actionType, resourceType, resourceId, AuditStatus.DENIED,
                    methodDetails + " | Access Denied: " + e.getMessage(), ipAddress, userAgent);
            throw e;

        } catch (Exception e) {
            // Log failure
            auditService.log(actionType, resourceType, resourceId, AuditStatus.FAILURE,
                    methodDetails + " | Error: " + e.getClass().getSimpleName() + " - " + e.getMessage(),
                    ipAddress, userAgent);
            throw e;
        }
    }

    private String extractResourceId(ProceedingJoinPoint joinPoint, Auditable auditable) {
        String expression = auditable.resourceIdExpression();
        if (expression == null || expression.isEmpty()) {
            return null;
        }

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
            Object[] args = joinPoint.getArgs();

            if (paramNames == null || paramNames.length == 0) {
                return null;
            }

            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                ((StandardEvaluationContext) context).setVariable(paramNames[i], args[i]);
            }

            Expression exp = spelParser.parseExpression(expression);
            Object result = exp.getValue(context);
            return result != null ? result.toString() : null;

        } catch (Exception e) {
            log.warn("Failed to extract resource ID using expression '{}': {}", expression, e.getMessage());
            return null;
        }
    }

    private String buildMethodDetails(ProceedingJoinPoint joinPoint, Auditable auditable) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        if (!auditable.includeArgs()) {
            return "Method: " + methodName;
        }

        String[] paramNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || args.length == 0) {
            return "Method: " + methodName + "()";
        }

        StringBuilder sb = new StringBuilder("Method: ").append(methodName).append("(");
        for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramNames[i]).append("=").append(summarizeArg(args[i]));
        }
        sb.append(")");

        return sb.toString();
    }

    private String summarizeArg(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof String s) {
            return s.length() > 50 ? "\"" + s.substring(0, 47) + "...\"" : "\"" + s + "\"";
        }
        if (arg instanceof byte[] bytes) {
            return "[byte array, length=" + bytes.length + "]";
        }
        if (arg.getClass().isArray()) {
            return "[array, length=" + Arrays.toString((Object[]) arg).length() + "]";
        }
        String str = arg.toString();
        return str.length() > 100 ? str.substring(0, 97) + "..." : str;
    }

    private String summarizeResult(Object result) {
        if (result == null) {
            return "null";
        }
        String str = result.toString();
        return str.length() > 200 ? str.substring(0, 197) + "..." : str;
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "unknown";
            }
            HttpServletRequest request = attrs.getRequest();

            // Check for forwarded headers (behind proxy/load balancer)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String userAgent = request.getHeader("User-Agent");
            return userAgent != null && userAgent.length() > 500
                    ? userAgent.substring(0, 497) + "..."
                    : userAgent;
        } catch (Exception e) {
            return null;
        }
    }
}
