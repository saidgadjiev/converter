package ru.gadjini.telegram.converter.dao.command.keyboard;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.ReplyKeyboardMarkup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Qualifier("inMemory")
public class InMemoryReplyKeyboardDao implements ReplyKeyboardDao {

    private Map<Long, ReplyKeyboardMarkup> cache = new ConcurrentHashMap<>();

    @Override
    public void store(long chatId, ReplyKeyboardMarkup replyKeyboardMarkup) {
        cache.put(chatId, replyKeyboardMarkup);
    }

    @Override
    public ReplyKeyboardMarkup get(long chatId) {
        return cache.get(chatId);
    }
}
