package com.tanglinlin.picture.backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.Assert;

/**
 * @program: tanglin-picture-backend
 * @ClassName RedisStringTest
 * @description:
 * @author: TSL
 * @create: 2025-09-08 17:49
 * @Version 1.0
 **/
@SpringBootTest
public class RedisStringTest {
    @Autowired
    private RedisTemplate redisTemplate;


    @Test
    public void testRedisStringOperation() {
        //获取操作对象
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        //key和value
        String testKey = "testKey";
        String testValue = "testValue";

        //1.测试新增或更新操作
        valueOperations.set(testKey, testValue);
        String storeValue = valueOperations.get(testKey);
        Assertions.assertEquals(testValue,
                storeValue, "存储的值和预期不一致");

        //2.测试修改操作
        String newValue = "newValue";
        valueOperations.set(testKey, newValue);
        storeValue = valueOperations.get(testKey);
        Assertions.assertEquals(newValue,
                storeValue, "存储的值和预期不一致");

        //3.测试查询操作
        storeValue = valueOperations.get(testKey);
        Assertions.assertNotNull(storeValue, "存储的值为空");
        Assertions.assertEquals(newValue,
                storeValue, "存储的值和预期不一致");

        //4.测试删除操作
        redisTemplate.delete(testKey);
        storeValue = valueOperations.get(testKey);
        Assertions.assertNull(storeValue, "存储的值不为空");
    }
}


