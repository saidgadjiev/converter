package ru.gadjini.telegram.converter.command.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.request.Arg;
import ru.gadjini.telegram.smart.bot.commons.command.api.CallbackBotCommand;
import ru.gadjini.telegram.smart.bot.commons.job.QueueJob;
import ru.gadjini.telegram.smart.bot.commons.model.bot.api.object.CallbackQuery;
import ru.gadjini.telegram.smart.bot.commons.service.request.RequestParams;

@Component
public class CancelQueryCommand implements CallbackBotCommand {

    private QueueJob conversionJob;

    @Autowired
    public CancelQueryCommand(QueueJob conversionJob) {
        this.conversionJob = conversionJob;
    }

    @Override
    public String getName() {
        return ConverterCommandNames.CANCEL_QUERY_COMMAND_NAME;
    }

    @Override
    public void processMessage(CallbackQuery callbackQuery, RequestParams requestParams) {
        int queryItemId = requestParams.getInt(Arg.QUEUE_ITEM_ID.getKey());
        conversionJob.cancel(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId(),
                callbackQuery.getId(), queryItemId);
    }
}
