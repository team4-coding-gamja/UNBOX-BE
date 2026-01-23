package com.example.unbox_common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    // 락의 이름 (Key - SpEL 사용 가능)
    String key();

    // 락 획득 대기 시간 (기본 5초)
    long waitTime() default 5L;

    // 락 점유 시간 (기본 3초)
    long leaseTime() default 3L;

    // 시간 단위
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
