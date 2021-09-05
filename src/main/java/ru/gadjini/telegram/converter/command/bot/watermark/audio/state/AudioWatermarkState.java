package ru.gadjini.telegram.converter.command.bot.watermark.audio.state;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.audio.AMarkCommand;

public interface AudioWatermarkState {

    void enter(Message message, Object... args);

    void update(AMarkCommand vMarkCommand, Message message, String text);

    AudioWatermarkStateName getName();
}
