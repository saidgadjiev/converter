package ru.gadjini.telegram.converter.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoState;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.job.ConversionWorkerFactory;
import ru.gadjini.telegram.converter.service.conversion.api.Any2AnyConverter;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageEvent;

@Component
public class MessageEventListener {

    private ConversionQueueService conversionQueueService;

    private CommandStateService commandStateService;

    private ConversionWorkerFactory conversionWorkerFactory;

    @Autowired
    public MessageEventListener(ConversionQueueService conversionQueueService, CommandStateService commandStateService,
                                ConversionWorkerFactory workerFactory) {
        this.conversionQueueService = conversionQueueService;
        this.commandStateService = commandStateService;
        conversionWorkerFactory = workerFactory;
    }

    @EventListener(MessageEvent.class)
    public void onEvent(MessageEvent messageEvent) {
        if (messageEvent.getEvent() instanceof ConversionCreatedEvent) {
            ConversionCreatedEvent createConversionEvent = (ConversionCreatedEvent) messageEvent.getEvent();
            Message message = (Message) messageEvent.getSendResult();

            ConversionQueueItem queueItem = conversionQueueService.getById(createConversionEvent.getQueueItemId());
            queueItem.setProgressMessageId(message.getMessageId());
            Any2AnyConverter candidate = conversionWorkerFactory.getCandidate(queueItem);

            int totalFilesToDownload = candidate.createDownloads(queueItem);
            conversionQueueService.setProgressMessageIdAndTotalFilesToDownload(createConversionEvent.getQueueItemId(),
                    message.getMessageId(), totalFilesToDownload);
        } else if (messageEvent.getEvent() instanceof AudioBassBoostSettingsSentEvent) {
            setConversionStateSettingsMessageId(ConverterCommandNames.BASS_BOOST, messageEvent);
        } else if (messageEvent.getEvent() instanceof AudioCompressionSettingsSentEvent) {
            setConversionStateSettingsMessageId(ConverterCommandNames.COMPRESS_AUDIO, messageEvent);
        } else if (messageEvent.getEvent() instanceof EditVideoSettingsSentEvent) {
            Message message = (Message) messageEvent.getSendResult();
            EditVideoState convertState = commandStateService.getState(message.getChatId(),
                    ConverterCommandNames.EDIT_VIDEO, false, EditVideoState.class);
            if (convertState != null) {
                convertState.getSettings().setMessageId(message.getMessageId());
                commandStateService.setState(message.getChatId(), ConverterCommandNames.EDIT_VIDEO, convertState);
            }
        }
    }

    private void setConversionStateSettingsMessageId(String commandName, MessageEvent messageEvent) {
        Message message = (Message) messageEvent.getSendResult();
        ConvertState convertState = commandStateService.getState(message.getChatId(),
                commandName, false, ConvertState.class);
        if (convertState != null) {
            convertState.getSettings().setMessageId(message.getMessageId());
            commandStateService.setState(message.getChatId(), commandName, convertState);
        }
    }
}
