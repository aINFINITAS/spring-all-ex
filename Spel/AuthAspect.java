package com.example.demo;

import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthAspect {

    private final PermissionService permissionService;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer =
            new DefaultParameterNameDiscoverer();

    @Before("@annotation(auth)")
    public void checkPermission(
            JoinPoint joinPoint,
            Auth auth
    ) throws BadRequestException {
        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();

        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // ✅ CREATE SpEL CONTEXT
        EvaluationContext context = new MethodBasedEvaluationContext(
                null,
                method,
                args,
                nameDiscoverer
        );
        // ===============================
        // ✅ 1. SAFE SpEL EVALUATION
        // ===============================
        String type = null;
        try {
            type = parser
                    .parseExpression(auth.type())
                    .getValue(context, String.class);
        } catch (Exception e) {
            log.error("SpEL parse error: {}", auth.type(), e);
            if (auth.defaultType() != null && !auth.defaultType().isBlank()) {
                type = auth.defaultType();
                log.warn("Use defaultType={} because SpEL eval failed", type);
            } else {
                throw new BadRequestException(
                        "INVALID AUTH TYPE EXPRESSION AND NO DEFAULT: " + auth.type()
                );
            }
        }

        log.info("TYPE FROM SpEL (raw): {}", type);

        // ===============================
        // ✅ 2. APPLY DEFAULT TYPE IF NULL / EMPTY
        // ===============================
        if (type == null || type.trim().isEmpty()) {

            if (auth.defaultType() != null && !auth.defaultType().isBlank()) {
                type = auth.defaultType();   // ✅ GÁN DEFAULT
                log.warn("Type is null → use defaultType={}", type);
            } else {
                log.warn("Missing type and no defaultType for method: {}", method.getName());
                throw new BadRequestException("TYPE IS REQUIRED");
            }
        }

        // ===============================
        // ✅ 3. TYPE DOES NOT EXIST IN THE SYSTEM
        // ===============================
        if (!permissionService.typeExists(type)) {
            log.warn("Unknown type: {}", type);
            throw new BadRequestException("UNKNOWN TYPE: " + type);
        }

        // ===============================
        // ✅ 4. CHECK PERMISSION
        // ===============================
        boolean passed = permissionService.hasPermission(
                type,
                auth.permission()
        );

        if (!passed) {
            log.warn("Permission denied. Type={}, Required={}",
                    type, String.join(",", auth.permission()));

            throw new AccessDeniedException(
                    "NO PERMISSION for type: " + type
            );
        }

        log.info("Auth passed. Type={}, Permission={}",
                type, String.join(",", auth.permission()));
    }
}
