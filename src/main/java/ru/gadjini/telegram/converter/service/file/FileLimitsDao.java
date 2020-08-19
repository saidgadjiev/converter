package ru.gadjini.telegram.converter.service.file;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
public class FileLimitsDao {

    private static final String KEY = "flim";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private StringRedisTemplate redisTemplate;

    @Autowired
    public FileLimitsDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setInputFile(long chatId, InputFileState inputFileState) {
        Map<String, String> values = new HashMap<>();

        values.put("state", inputFileState.getState().name());
        if (inputFileState.getReplyToMessageId() != null) {
            values.put("replyToMessageId", inputFileState.getReplyToMessageId().toString());
        }
        if (inputFileState.getFileId() != null) {
            values.put("fileId", inputFileState.getFileId());
        }
        values.put("command", inputFileState.getCommand());
        values.put("createdAt", DATE_TIME_FORMATTER.format(inputFileState.getCreatedAt()));

        redisTemplate.opsForHash().putAll(key(chatId), values);
    }

    public void setInputFileTtl(long chatId, long ttl, TimeUnit timeUnit) {
        redisTemplate.expire(key(chatId), ttl, timeUnit);
    }

    public boolean hasInputFile(long chatId) {
        return BooleanUtils.toBoolean(redisTemplate.hasKey(key(chatId)));
    }

    public Long getInputFileTtl(long chatId) {
        return redisTemplate.getExpire(key(chatId));
    }

    public void deleteInputFile(long chatId) {
        redisTemplate.delete(key(chatId));
    }

    public InputFileState getInputFile(long chatId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(chatId));

        if (entries.isEmpty()) {
            return null;
        }
        InputFileState inputFileState = new InputFileState();
        String replyToMessageId = (String) entries.get(InputFileState.REPLY_TO_MESSAGE_ID);
        inputFileState.setReplyToMessageId(replyToMessageId == null ? null : Integer.parseInt(replyToMessageId));
        inputFileState.setState(InputFileState.State.valueOf((String) entries.get(InputFileState.STATE)));

        return inputFileState;
    }

    public void setState(long chatId, InputFileState.State state) {
        redisTemplate.opsForHash().put(key(chatId), InputFileState.STATE, state.name());
    }

    private String key(long chatId) {
        return KEY + ":" + chatId;
    }
}
