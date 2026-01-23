package com.example.unbox_common.lock;

import com.example.unbox_common.error.exception.LockAcquisitionFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class DistributedLockAop {

    private static final String REDISSON_LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

    @Around("@annotation(com.example.unbox_common.lock.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        // Key 파싱 (SpEL)
        String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key()).toString();
        RLock rLock = redissonClient.getLock(key);

        try {
            boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            if (!available) {
                log.warn("Redisson Lock Acquisition Failed/Timeout. Key: {}", key);
                // 락 획득 실패 시 false 반환 또는 예외 던짐. 
                // 서비스 성격에 따라 다르지만, 여기서는 false 반환 보다는 예외를 던져서 트랜잭션을 롤백시키는 게 안전 (409 Conflict 등 매핑 가능)
                throw new LockAcquisitionFailedException(key, "Redisson Lock Acquisition Failed");
            }

            // 트랜잭션 분리 실행
            return aopForTransaction.proceed(joinPoint);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Redisson Lock Interrupted", e);
            throw e;
        } finally {
            try {
                if (rLock.isHeldByCurrentThread()) {
                    rLock.unlock();
                }
            } catch (IllegalMonitorStateException e) {
                log.warn("Redisson Lock already unlocked: {}", key);
            }
        }
    }
}
