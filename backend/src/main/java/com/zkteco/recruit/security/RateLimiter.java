package com.zkteco.recruit.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.zkteco.recruit.common.BizException;
import com.zkteco.recruit.common.ErrorCode;
import com.zkteco.recruit.config.AppProperties;

/**
 * 最小限流（§16.2）：登录失败锁定 + 按小时计数。
 * <p>
 * 内存实现，仅适用于单实例部署（与 D7、Q5 的单实例约束一致）。
 */
@Component
public class RateLimiter {

    private final AppProperties appProperties;

    /** key -> 失败次数与首次失败时间 */
    private final Map<String, FailRecord> loginFailures = new ConcurrentHashMap<>();
    /** key -> 小时窗口计数 */
    private final Map<String, WindowCounter> hourlyCounters = new ConcurrentHashMap<>();

    public RateLimiter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /* ---------------- 登录失败锁定 ---------------- */

    public void assertLoginNotLocked(String email) {
        FailRecord record = loginFailures.get(key(email));
        if (record == null) {
            return;
        }
        Duration lock = Duration.ofMinutes(appProperties.getSecurity().getLoginLockMinutes());
        if (record.count.get() >= appProperties.getSecurity().getLoginFailThreshold()) {
            Instant unlockAt = record.lastFailAt.plus(lock);
            if (Instant.now().isBefore(unlockAt)) {
                long minutes = Math.max(1, Duration.between(Instant.now(), unlockAt).toMinutes() + 1);
                throw new BizException(ErrorCode.LOGIN_LOCKED,
                        "登录失败次数过多，请在 " + minutes + " 分钟后重试");
            }
            loginFailures.remove(key(email));
        }
    }

    public void recordLoginFailure(String email) {
        FailRecord record = loginFailures.computeIfAbsent(key(email), k -> new FailRecord());
        Duration lock = Duration.ofMinutes(appProperties.getSecurity().getLoginLockMinutes());
        if (Instant.now().isAfter(record.lastFailAt.plus(lock))) {
            record.count.set(0);
        }
        record.count.incrementAndGet();
        record.lastFailAt = Instant.now();
    }

    public void clearLoginFailure(String email) {
        loginFailures.remove(key(email));
    }

    /* ---------------- 小时窗口计数 ---------------- */

    public void assertHourlyLimit(String scope, String identity, int limit, String message) {
        String key = scope + ":" + key(identity);
        WindowCounter counter = hourlyCounters.computeIfAbsent(key, k -> new WindowCounter());
        synchronized (counter) {
            if (Instant.now().isAfter(counter.windowStart.plus(Duration.ofHours(1)))) {
                counter.windowStart = Instant.now();
                counter.count.set(0);
            }
            if (counter.count.get() >= limit) {
                throw new BizException(ErrorCode.TOO_MANY_REQUESTS, message);
            }
            counter.count.incrementAndGet();
        }
    }

    private String key(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }

    private static class FailRecord {
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant lastFailAt = Instant.now();
    }

    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant windowStart = Instant.now();
    }
}
