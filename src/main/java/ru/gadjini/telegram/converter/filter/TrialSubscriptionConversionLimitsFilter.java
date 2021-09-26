package ru.gadjini.telegram.converter.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.service.queue.ConversionQueueService;
import ru.gadjini.telegram.smart.bot.commons.domain.PaidSubscription;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.filter.BaseBotFilter;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.model.TgMessage;
import ru.gadjini.telegram.smart.bot.commons.property.SubscriptionProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.MessageMediaService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.subscription.FatherPaidSubscriptionService;

import java.util.Locale;

@Component
public class TrialSubscriptionConversionLimitsFilter extends BaseBotFilter {

    private FatherPaidSubscriptionService commonPaidSubscriptionService;

    private SubscriptionProperties subscriptionProperties;

    private MessageMediaService messageMediaService;

    private ConversionQueueService conversionQueueService;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public TrialSubscriptionConversionLimitsFilter(FatherPaidSubscriptionService commonPaidSubscriptionService,
                                                   SubscriptionProperties subscriptionProperties,
                                                   MessageMediaService messageMediaService,
                                                   ConversionQueueService conversionQueueService,
                                                   LocalisationService localisationService, UserService userService) {
        this.commonPaidSubscriptionService = commonPaidSubscriptionService;
        this.subscriptionProperties = subscriptionProperties;
        this.messageMediaService = messageMediaService;
        this.conversionQueueService = conversionQueueService;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public void doFilter(Update update) {
        long chatId = TgMessage.getChatId(update);
        PaidSubscription subscription = commonPaidSubscriptionService.getSubscription(chatId);

        if (update.hasMessage()
                && subscriptionProperties.isCheckPaidSubscription()
                && isFreeSubscription(subscription)) {
            checkFreeUserLimits(update.getMessage());
        }

        super.doFilter(update);
    }

    private void checkFreeUserLimits(Message message) {
        MessageMedia media = messageMediaService.getMedia(message, Locale.getDefault());

        if (media == null) {
            return;
        }
        Locale locale = userService.getLocaleOrDefault(message.getFrom().getId());
        if (media.getFileSize() == 0) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_TRIAL_USER_ZERO_SIZE_LIMIT, locale));
        }
        if (media.getFileSize() > subscriptionProperties.getTrialMaxFileSize()) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_TRIAL_USER_SIZE_LIMIT, locale));
        }
        if (conversionQueueService.countByUser(message.getFrom().getId()) >= subscriptionProperties.getTrialMaxActionsCount()) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_TRIAL_USER_COUNT_LIMIT, locale));
        }
    }

    private boolean isFreeSubscription(PaidSubscription paidSubscription) {
        return paidSubscription == null || paidSubscription.isTrial();
    }
}
