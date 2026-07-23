package org.javaup.lease;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 管理组件
 * @author: 阿星不是程序员
 **/

public class RedisLeaseManager {

    private static final String ACQUIRE_SCRIPT =
        "if redis.call('exists', KEYS[1]) == 0 then "
            + "redis.call('psetex', KEYS[1], ARGV[2], ARGV[1]); "
            + "return 1; "
            + "end; "
            + "return 0;";

    private static final String RENEW_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then "
            + "redis.call('pexpire', KEYS[1], ARGV[2]); "
            + "return 1; "
            + "end; "
            + "return 0;";

    private static final String RELEASE_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then "
            + "return redis.call('del', KEYS[1]); "
            + "end; "
            + "return 0;";

    private final RedissonClient redissonClient;

    public RedisLeaseManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public boolean acquire(String key, String ownerToken, Duration ttl) {
        return executeInteger(ACQUIRE_SCRIPT, key, ownerToken, ttl) == 1L;
    }

    public boolean renew(String key, String ownerToken, Duration ttl) {
        return executeInteger(RENEW_SCRIPT, key, ownerToken, ttl) == 1L;
    }

    public boolean release(String key, String ownerToken) {
        Assert.hasText(key, "key 不能为空");
        Assert.hasText(ownerToken, "ownerToken 不能为空");
        Long result = redissonClient.getScript(StringCodec.INSTANCE).eval(
            RScript.Mode.READ_WRITE,
            RELEASE_SCRIPT,
            RScript.ReturnType.INTEGER,
            List.of(key),
            ownerToken
        );
        return result != null && result == 1L;
    }

    private long executeInteger(String script, String key, String ownerToken, Duration ttl) {

        Assert.hasText(key, "key 不能为空");
        Assert.hasText(ownerToken, "ownerToken 不能为空");
        Assert.notNull(ttl, "ttl 不能为空");
        Assert.isTrue(!ttl.isNegative() && !ttl.isZero(), "ttl 必须大于 0");

        Long result = redissonClient.getScript(StringCodec.INSTANCE).eval(
            RScript.Mode.READ_WRITE,
            script,
            RScript.ReturnType.INTEGER,
            List.of(key),
            ownerToken,
            String.valueOf(ttl.toMillis())
        );
        return result != null ? result : 0L;
    }
}
