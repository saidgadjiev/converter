package ru.gadjini.telegram.converter.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.domain.watermark.audio.AudioWatermark;
import ru.gadjini.telegram.converter.service.watermark.audio.AudioWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;

@Component
public class ViewAudioWatermarkAudioCommand implements BotCommand {

    private AudioWatermarkService audioWatermarkService;

    private MediaMessageService messageMediaService;

    @Autowired
    public ViewAudioWatermarkAudioCommand(AudioWatermarkService audioWatermarkService,
                                          @Qualifier("mediaLimits") MediaMessageService messageMediaService) {
        this.audioWatermarkService = audioWatermarkService;
        this.messageMediaService = messageMediaService;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        AudioWatermark watermark = audioWatermarkService.getWatermark(message.getFrom().getId());

        messageMediaService.sendFile(message.getChatId(), watermark.getAudio().getFileId());
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.AMARKAUDIO;
    }
}
