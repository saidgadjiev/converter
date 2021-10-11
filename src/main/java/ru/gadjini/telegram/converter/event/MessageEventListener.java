package ru.gadjini.telegram.converter.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.smart.bot.commons.domain.CreateOrUpdateResult;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageEvent;

@Component
public class MessageEventListener {

    private ConversionQueueService conversionQueueService;

    private CommandStateService commandStateService;

    @Autowired
    public MessageEventListener(ConversionQueueService conversionQueueService, CommandStateService commandStateService) {
        this.conversionQueueService = conversionQueueService;
        this.commandStateService = commandStateService;
    }

    @EventListener(MessageEvent.class)
    public void onEvent(MessageEvent messageEvent) {
        if (messageEvent.getEvent() instanceof CreateOrUpdateResult) {
            ConversionCreatedEvent createConversionEvent = (ConversionCreatedEvent) messageEvent.getEvent();
            Message message = (Message) messageEvent.getSendResult();
            conversionQueueService.setProgressMessageId(createConversionEvent.getQueueItemId(), message.getMessageId());
        } else if (messageEvent.getEvent() instanceof AudioBassBoostSettingsSentEvent) {
            setConversionSettingsMessageId(messageEvent);
        } else if (messageEvent.getEvent() instanceof AudioCompressionSettingsSentEvent) {
            setConversionSettingsMessageId(messageEvent);
        } else if (messageEvent.getEvent() instanceof EditVideoSettingsSentEvent) {
            setConversionSettingsMessageId(messageEvent);
        }
    }

    private void setConversionSettingsMessageId(MessageEvent messageEvent) {
        Message message = (Message) messageEvent.getSendResult();
        ConvertState convertState = commandStateService.getState(message.getChatId(),
                ConverterCommandNames.BASS_BOOST, false, ConvertState.class);
        if (convertState != null) {
            convertState.getSettings().setMessageId(message.getMessageId());
            commandStateService.setState(message.getChatId(), ConverterCommandNames.BASS_BOOST, convertState);
        }
    }
}
