package ru.gadjini.telegram.converter.dao.command.navigator.keyboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Qualifier("redis")
public class RedisCommandNavigatorDao implements CommandNavigatorDao {

    private static final String CURRENT_KEY = "keyboard:command:navigator:current";

    private StringRedisTemplate redisTemplate;

    @Autowired
    public RedisCommandNavigatorDao(StringRedisTemplate redisTemplate) {
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

}
