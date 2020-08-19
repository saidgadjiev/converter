package ru.gadjini.telegram.converter.dao;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.converter.service.message.MessageService;

import java.util.concurrent.TimeUnit;

@Repository
public class SubscriptionDao {

    private static final String KEY = "sub";

    private StringRedisTemplate stringRedisTemplate;

    private MessageService messageService;

    @Autowired
    public SubscriptionDao(StringRedisTemplate stringRedisTemplate, @Qualifier("messagelimits") MessageService messageService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.messageService = messageService;
    }

    public boolean isChatMember(String chatId, int userId, long ttl, TimeUnit timeUnit) {
        String key = key(userId);
        boolean aBoolean = BooleanUtils.toBoolean(stringRedisTemplate.hasKey(key));

        if (aBoolean) {
            return true;
        } else {
            boolean chatMember = messageService.isChatMember(chatId, userId);

            if (chatMember) {
                stringRedisTemplate.opsForValue().set(key, Boolean.TRUE.toString());
                stringRedisTemplate.expire(key, ttl, timeUnit);

                return true;
            } else {
                return false;
            }
        }
    }

    private String key(int userId) {
        return KEY + ":" + userId;
    }
}
