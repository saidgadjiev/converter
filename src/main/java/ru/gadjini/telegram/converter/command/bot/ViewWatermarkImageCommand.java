package ru.gadjini.telegram.converter.command.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermark;
import ru.gadjini.telegram.converter.service.watermark.video.VideoWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.command.api.BotCommand;
import ru.gadjini.telegram.smart.bot.commons.service.message.MediaMessageService;

@Component
public class ViewWatermarkImageCommand implements BotCommand {

    private VideoWatermarkService videoWatermarkService;

    private MediaMessageService messageMediaService;

    @Autowired
    public ViewWatermarkImageCommand(VideoWatermarkService videoWatermarkService,
                                     @Qualifier("mediaLimits") MediaMessageService messageMediaService) {
        this.videoWatermarkService = videoWatermarkService;
        this.messageMediaService = messageMediaService;
    }

    @Override
    public void processMessage(Message message, String[] params) {
        VideoWatermark watermark = videoWatermarkService.getWatermark(message.getFrom().getId());

        switch (watermark.getWatermarkType()) {
            case IMAGE:
            case VIDEO:
            case GIF:
                messageMediaService.sendFile(message.getChatId(), watermark.getImage().getFileId());
                break;
            case STICKER:
                messageMediaService.sendSticker(SendSticker.builder().sticker(new InputFile(watermark.getImage().getFileId())).build());
                break;
        }
    }

    @Override
    public String getCommandIdentifier() {
        return ConverterCommandNames.VMARKIMG;
    }
}
