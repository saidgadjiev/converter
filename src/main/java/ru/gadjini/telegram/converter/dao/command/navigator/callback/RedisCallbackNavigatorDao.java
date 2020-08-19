package ru.gadjini.telegram.converter.dao.command.navigator.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Qualifier("redis")
public class RedisCallbackNavigatorDao implements CallbackCommandNavigatorDao {

    private static final String CURRENT_KEY = "callback:command:navigator:current";

    private StringRedisTemplate redisTemplate;

    @Autowired
    public RedisCallbackNavigatorDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void set(long chatId, String command) {
        redisTemplate.opsForHash().put(CURRENT_KEY, String.valueOf(chatId), command);
    }

    @Override
    public String get(long chatId) {
        return (String) redisTemplate.opsForHash().get(CURRENT_KEY, String.valueOf(chatId));
    }

    @Override
    public void delete(long chatId) {
        redisTemplate.opsForHash().delete(CURRENT_KEY, String.valueOf(chatId));
    }
}
