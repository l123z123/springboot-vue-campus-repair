package com.campus.repair.aspect;

import com.campus.repair.annotation.OperationLog;
import com.campus.repair.context.UserContext;
import com.campus.repair.entity.OperationLogRecord;
import com.campus.repair.mapper.OperationLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogMapper operationLogMapper;

    public OperationLogAspect(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Around("@annotation(op)")
    public Object around(ProceedingJoinPoint pjp, OperationLog op) throws Throwable {
        String action = op.value().isEmpty() ? methodName(pjp) : op.value();
        Long operatorId = UserContext.getUserId();
        String ip = getClientIp();

        Object result = pjp.proceed();

        try {
            OperationLogRecord entity = new OperationLogRecord();
            entity.setOperatorId(operatorId);
            entity.setAction(action);
            entity.setIp(ip);
            entity.setParams(paramsSummary(pjp));
            entity.setCreateTime(LocalDateTime.now());
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("操作日志写入失败: {}", e.getMessage());
        }
        return result;
    }

    private String methodName(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        return sig.getDeclaringType().getSimpleName() + "." + sig.getName();
    }

    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest req = attrs.getRequest();
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private String paramsSummary(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(args.length, 3); i++) {
            if (i > 0) sb.append(", ");
            String s = args[i] != null ? args[i].toString() : "null";
            if (s.length() > 100) s = s.substring(0, 100) + "...";
            sb.append(s);
        }
        return sb.toString();
    }
}
