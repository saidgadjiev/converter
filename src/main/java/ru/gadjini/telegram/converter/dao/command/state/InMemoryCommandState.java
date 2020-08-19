package ru.gadjini.telegram.converter.dao.command.state;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Repository
@Qualifier("inMemory")
public class InMemoryCommandState implements CommandStateDao {

    private Map<Long, Object> states = new ConcurrentHashMap<>();

    @Override
    public void setState(long chatId, String command, Object state, long ttl, TimeUnit timeUnit) {
        states.put(chatId, state);
    }

    @Override
    public <T> T getState(long chatId, String command, Class<T> tClass) {
        return (T) states.get(chatId);
    }

    @Override
    public boolean hasState(long chatId, String command) {
        return states.containsKey(chatId);
    }

    @Override
    public void deleteState(long chatId, String command) {
        states.remove(chatId);
    }

}
