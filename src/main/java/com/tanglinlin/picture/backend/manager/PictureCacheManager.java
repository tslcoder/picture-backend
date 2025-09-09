package com.tanglinlin.picture.backend.manager;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.tanglinlin.picture.backend.exception.BusinessException;
import com.tanglinlin.picture.backend.exception.ErrorCode;
import com.tanglinlin.picture.backend.generator.domain.Picture;
import com.tanglinlin.picture.backend.generator.service.PictureService;
import com.tanglinlin.picture.backend.model.dto.picture.PictureQueryRequest;
import com.tanglinlin.picture.backend.model.vo.PictureVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @program: tanglin-picture-backend
 * @ClassName PictureCacheManager
 * @description:
 * @author: TSL
 * @create: 2025-09-09 10:10
 * @Version 1.0
 **/
@Component
@Slf4j
public class PictureCacheManager {
    @Resource
    private PictureService pictureService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 缓存key
     */
    private static final String PAGE_PICTURE_CACHE_KEY = "TangLin-PicHub:page:%s";
    /**
     * 分布式锁的key
     */
    private static final String PICTURE_LOCK_KEY = "TangLin-PicHub:picture:lock:%s";

    /**
     * 最小过期时间
     */
    private static final Long MIN_EXPIRE_TIME = 5L;

    /**
     * 最大过期时间
     */
    private static final Long MAX_EXPIRE_TIME = 60L;

    /**
     * 过期时间单位
     */
    private static final TimeUnit EXPIRE_TIME_UNIT = TimeUnit.MINUTES;

    // Lua 原子解锁脚本（可静态常量）
    private static final String UNLOCK_LUA = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            else
              return 0
            end
            """;


    /**
     * 本地缓存
     */

    private static final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1000)
            .maximumSize(10000)
            //自定义过期时间
            //随机过期时间
            //避免缓存雪崩
            .expireAfter(new Expiry<>() {
                @Override
                public long expireAfterCreate(Object o, Object o2, long l) {
                    return EXPIRE_TIME_UNIT.toNanos(RandomUtil.randomLong(MIN_EXPIRE_TIME, MAX_EXPIRE_TIME));
                }

                @Override
                public long expireAfterUpdate(Object o, Object o2, long l, @NonNegative long l1) {
                    return l1;
                }

                @Override
                public long expireAfterRead(Object o, Object o2, long l, @NonNegative long l1) {
                    return l1;
                }
            }).build();

    public Page<PictureVO> getPagePictureByCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest req) {
        // 1) 生成 key
        String reqJson = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(reqJson.getBytes());
        String dataKey = String.format(PAGE_PICTURE_CACHE_KEY, hashKey);
        String lockKey = String.format(PICTURE_LOCK_KEY, hashKey);

        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // 2) 先读本地/Redis，命中即回填本地并返回
        Page<PictureVO> cached = tryGetPagePictureCache(dataKey, ops);
        if (cached != null) return cached;

        // 3) 抢锁
        String token = UUID.randomUUID() + ":" + Thread.currentThread().getId();
        int maxRetry = 6;                 // 可配置
        long baseSleep = 80;              // ms
        int attempt = 0;

        try {
            while (true) {
                Boolean ok = ops.setIfAbsent(lockKey, token, 30, TimeUnit.SECONDS); // 锁期略长
                if (Boolean.TRUE.equals(ok)) break;

                // 退避 + 抖动
                long sleep = (long) (baseSleep * Math.pow(2, Math.min(attempt, 4)));
                sleep += RandomUtil.randomLong(0, 30);
                Thread.sleep(sleep);

                // 再查缓存，命中则返回
                cached = tryGetPagePictureCache(dataKey, ops);
                if (cached != null) return cached;

                if (++attempt >= maxRetry) {
                    // 降级策略：直接查库但不写缓存，或写入空值短 TTL
                    return fallbackQueryWithoutCache(pictureQueryRequest, req);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "线程中断，获取分布式锁失败");
        }

        // 4) 拿到锁 → 双检
        try {
            cached = tryGetPagePictureCache(dataKey, ops);
            if (cached != null) return cached;

            // 入参校验与限流
            int pageSize = pictureQueryRequest.getPageSize();
            int current = pictureQueryRequest.getCurrent();

            Page<Picture> page = new Page<>(current, pageSize);
            Page<Picture> picturePage = pictureService.page(page, pictureService.getQueryWrapper(pictureQueryRequest));

            Page<PictureVO> voPage = pictureService.getPictureVOPage(picturePage, req);

            // 空值占位，短 TTL 减少穿透
            String json = JSONUtil.toJsonStr(voPage);
            long ttlMin = MIN_EXPIRE_TIME;
            long ttlMax = MAX_EXPIRE_TIME;
            if (voPage.getRecords() == null || voPage.getRecords().isEmpty()) {
                ttlMin = 1L; ttlMax = 3L; // 短 TTL
            }

            long ttl = RandomUtil.randomLong(ttlMin, ttlMax);
            ops.set(dataKey, json, ttl, EXPIRE_TIME_UNIT);

            // 回填本地缓存
            LOCAL_CACHE.put(dataKey, json);

            return voPage;
        } finally {
            // 5) 原子解锁
            try {
                redisTemplate.execute((connection) ->
                                connection.scriptingCommands().eval(
                                        UNLOCK_LUA.getBytes(),
                                        ReturnType.INTEGER, 1,
                                        lockKey.getBytes(),
                                        token.getBytes()
                                )
                        , true, true);
            } catch (Exception e) {
                log.warn("释放分布式锁异常 key={}, err={}", lockKey, e.toString());
            }
        }
    }

    private Page<PictureVO> fallbackQueryWithoutCache(PictureQueryRequest req, HttpServletRequest httpReq) {
        int pageSize = req.getPageSize();
        int current = req.getCurrent();
        Page<Picture> page = new Page<>(current, pageSize);
        Page<Picture> picPage = pictureService.page(page, pictureService.getQueryWrapper(req));
        return pictureService.getPictureVOPage(picPage, httpReq);
    }

    private Page<PictureVO> tryGetPagePictureCache(String key, ValueOperations<String, String> ops) {
        String local = LOCAL_CACHE.getIfPresent(key);
        if (ObjectUtil.isNotNull(local)) return picStrToPage(local);

        String redisVal = ops.get(key);
        if (ObjectUtil.isNotNull(redisVal)) {
            // 回填本地
            LOCAL_CACHE.put(key, redisVal);
            return picStrToPage(redisVal);
        }
        return null;
    }

    private Page<PictureVO> picStrToPage(String picStr) {
        Page<PictureVO> page = JSONUtil.toBean(picStr, new TypeReference<>() {
        }, false);
        List<PictureVO> records = JSONUtil.toList(JSONUtil.parseArray(page.getRecords()), PictureVO.class);
        page.setRecords(records);
        return page;
    }
}
