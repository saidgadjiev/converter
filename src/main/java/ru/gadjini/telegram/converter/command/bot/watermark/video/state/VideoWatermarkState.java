package ru.gadjini.telegram.converter.command.bot.watermark.video.state;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.watermark.video.VMarkCommand;

public interface VideoWatermarkState {

    void enter(Message message, Object... args);

    void update(VMarkCommand vMarkCommand, Message message, String text);

    WatermarkStateName getName();
}
