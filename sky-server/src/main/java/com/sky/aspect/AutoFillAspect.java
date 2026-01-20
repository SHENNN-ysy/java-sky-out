package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Component
@Aspect
public class AutoFillAspect {
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void AutoFillPointCut() {
    }

    //前置通知
    @Before("AutoFillPointCut()")
    public void AutoFill(JoinPoint joinPoint){
        log.info("开始公共字段的自动填充");

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取方法上的AutoFill注解
        AutoFill autoFill = method.getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object entity = args[0];
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 获取当前操作人ID
        Long currentId = BaseContext.getCurrentId();

        if (operationType == OperationType.INSERT) {
            // 插入操作需要填充的字段
            setFieldVal(entity, AutoFillConstant.SET_CREATE_TIME, now);
            setFieldVal(entity, AutoFillConstant.SET_UPDATE_TIME, now);
            setFieldVal(entity, AutoFillConstant.SET_CREATE_USER, currentId);
            setFieldVal(entity, AutoFillConstant.SET_UPDATE_USER, currentId);
        } else if (operationType == OperationType.UPDATE) {
            // 更新操作需要填充的字段
            setFieldVal(entity, AutoFillConstant.SET_UPDATE_TIME, now);
            setFieldVal(entity, AutoFillConstant.SET_UPDATE_USER, currentId);
        }
    }

    /**
     * 通过反射设置实体类字段的值
     */
    private void setFieldVal(Object entity, String methodName, Object value) {
        try {
            Method method = entity.getClass().getMethod(methodName, value.getClass());
            method.invoke(entity, value);
        } catch (Exception e) {
            log.error("反射设置字段值失败: {}", e.getMessage());
        }
    }
}
