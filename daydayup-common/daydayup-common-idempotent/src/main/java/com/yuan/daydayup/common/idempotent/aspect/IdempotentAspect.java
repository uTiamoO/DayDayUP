package com.yuan.daydayup.common.idempotent.aspect;

import com.yuan.daydayup.common.core.context.UserContextHolder;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.idempotent.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/**
 * 幂等切面
 *
 * <p>使用 Redis {@code SET key value NX EX ttl} 原子占位：</p>
 * <ul>
 *   <li>成功 → 业务方法继续执行</li>
 *   <li>失败（已存在）→ 抛 {@link ErrorCode#REPEAT_SUBMIT}</li>
 * </ul>
 *
 * <p>Key 自动拼接前缀 {@code daydayup:idempotent:<userId>:<sourceKey>}，
 * 保证不同用户的相同业务键不会互相冲突。</p>
 */
@Aspect
public class IdempotentAspect {

    private static final String KEY_PREFIX = "daydayup:idempotent:";

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final StringRedisTemplate redisTemplate;

    public IdempotentAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = buildKey(joinPoint, idempotent);
        Duration ttl = Duration.ofMillis(idempotent.unit().toMillis(idempotent.expire()));
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BizException(ErrorCode.REPEAT_SUBMIT.getCode(), idempotent.message());
        }
        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        Long userId = UserContextHolder.getUserId();
        String userPart = userId == null ? "anon" : String.valueOf(userId);

        String source = idempotent.key();
        String resolved;
        if (source == null || source.isBlank()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            resolved = signature.getDeclaringTypeName() + "#" + signature.getName()
                    + ":" + Math.abs(Objects.hash(Arrays.deepHashCode(joinPoint.getArgs())));
        } else {
            resolved = evaluateSpEL(source, joinPoint);
        }
        return KEY_PREFIX + userPart + ":" + resolved;
    }

    private String evaluateSpEL(String expression, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);

        EvaluationContext ctx = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }
        Expression exp = PARSER.parseExpression(expression);
        Object value = exp.getValue(ctx);
        return value == null ? "null" : String.valueOf(value);
    }
}
