package com.atguigu.gmall.Index.utils;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 加锁(重试)
 * 解锁
 * 续期
 */

@Component
public class DistributedLock {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Thread thread;

    // 加锁
    public Boolean tryLock(String LockName,String uuid,Long expire) {
        String script = "if (redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1) " +
                "then redis.call('hincrby', KEYS[1], ARGV[1], 1); redis.call('expire', KEYS[1], ARGV[2]); return 1; " +
                "else return 0; end;";
        if (this.stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(LockName), uuid, expire.toString()) == 0) {
            try {
                // 没有获取到锁，重试
                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
            tryLock(LockName,uuid,expire);
        }
        //续期
        renewTime(LockName,uuid,expire);
        // 获取到锁，返回true
        return true;
    }

    // 解锁
    public void unLock(String LockName,String uuid) {
        String script = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return nil end; " +
                "if (redis.call('hincrby', KEYS[1], ARGV[1], -1) > 0) then return 0 " +
                "else redis.call('del', KEYS[1]) return 1 end;";

        if (this.stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(LockName), uuid) == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, lockName: " + LockName + " uuid: "  + uuid);
        }
        //关闭线程
        this.thread.interrupt();
    }


    //续期
    public void renewTime(String lockName, String uuid,Long expire) {
        String script = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 1) " +
                "then return redis.call('expire', KEYS[1], ARGV[2]) end;";
        this.thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(expire * 1000 * 2 / 3);
                    this.stringRedisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Arrays.asList(lockName),uuid,expire.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        this.thread.start();
    }


}
