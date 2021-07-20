package ru.gadjini.telegram.converter.service.caption;

import ru.gadjini.telegram.smart.bot.commons.domain.FileSource;

public interface CaptionGenerator {

    default String generate(long userId, FileSource fileSource) {
        return generate(userId, fileSource, null);
    }

    String generate(long userId, FileSource fileSource, String currentCaption);
}
