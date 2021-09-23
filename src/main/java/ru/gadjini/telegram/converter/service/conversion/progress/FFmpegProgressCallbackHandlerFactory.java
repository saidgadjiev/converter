package ru.gadjini.telegram.converter.service.conversion.progress;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.queue.ConversionStep;
import ru.gadjini.telegram.smart.bot.commons.annotation.TelegramMessageLimitsControl;
import ru.gadjini.telegram.smart.bot.commons.property.ProgressProperties;
import ru.gadjini.telegram.smart.bot.commons.service.keyboard.SmartInlineKeyboardService;
import ru.gadjini.telegram.smart.bot.commons.service.message.MessageService;
import ru.gadjini.telegram.smart.bot.commons.service.process.FFmpegProgressCallback;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FFmpegProgressCallbackHandlerFactory {

    private ConversionMessageBuilder messageBuilder;

    private MessageService messageService;

    private ProgressProperties progressProperties;

    private SmartInlineKeyboardService smartInlineKeyboardService;

    @Autowired
    protected FFmpegProgressCallbackHandlerFactory(ConversionMessageBuilder messageBuilder,
                                                   @TelegramMessageLimitsControl MessageService messageService,
                                                   ProgressProperties progressProperties,
                                                   SmartInlineKeyboardService smartInlineKeyboardService) {
        this.messageBuilder = messageBuilder;
        this.messageService = messageService;
        this.progressProperties = progressProperties;
        this.smartInlineKeyboardService = smartInlineKeyboardService;
    }

    public FFmpegProgressCallbackHandler createCallback(ConversionQueueItem queueItem, Long duration, Locale locale) {
        return new FFmpegProgressCallbackHandler(duration, progressProperties.getTimeToUpdate(), queueItem, locale);
    }

    public class FFmpegProgressCallbackHandler implements FFmpegProgressCallback {

        public final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
                .printZeroIfSupported()
                .minimumPrintedDigits(2)
                .rejectSignedValues(true)
                .appendHours()
                .appendSeparator(":")
                .appendMinutes()
                .appendSeparator(":")
                .appendSeconds()
                .toFormatter();

        private final AtomicLong start = new AtomicLong(0);

        private Long duration;

        private long timeToUpdate;

        private ConversionQueueItem queueItem;

        private Locale locale;

        private AtomicInteger lastPercentage = new AtomicInteger(-1);

        FFmpegProgressCallbackHandler(Long duration, long timeToUpdate, ConversionQueueItem queueItem, Locale locale) {
            this.duration = duration;
            this.timeToUpdate = timeToUpdate;
            this.queueItem = queueItem;
            this.locale = locale;
        }

        @Override
        public void progress(int timeLeft, int percentage, Double speed) {
            if (isStartPoint() | isBadPercentage(percentage)) {
                return;
            }
            if (isTimeToUpdate()) {
                int eta = (int) (timeLeft / speed);
                String etaString = eta > 0 ? PERIOD_FORMATTER.print(Period.seconds((int) (timeLeft / speed)).normalizedStandard()) : "N/A";
                String speedString = speed == null ? "N/A" : speed + "x";

                updateProgressMessage(etaString, speedString, percentage);
            }
        }

        @Override
        public Long duration() {
            return duration;
        }

        private boolean isBadPercentage(int percentage) {
            int lastPercent = lastPercentage.get();
            lastPercentage.set(percentage);

            return lastPercent >= percentage;
        }

        private boolean isTimeToUpdate() {
            long epochSecond = Instant.now().getEpochSecond();
            long diff = epochSecond - start.get();
            if (diff >= timeToUpdate) {
                start.set(epochSecond);

                return true;
            }

            return false;
        }

        private boolean isStartPoint() {
            if (start.get() == 0) {
                start.set(Instant.now().getEpochSecond());

                return true;
            }

            return false;
        }

        private void updateProgressMessage(String eta, String speed, int percentage) {
            String conversionProcessingMessage = messageBuilder.getConversionProcessingMessage(
                    queueItem, ConversionStep.CONVERTING, Set.of(ConversionStep.DOWNLOADING), true, true,
                    locale
            );

            messageService.editMessage(
                    EditMessageText.builder()
                            .messageId(queueItem.getProgressMessageId())
                            .chatId(String.valueOf(queueItem.getUserId()))
                            .text(String.format(conversionProcessingMessage, percentage, eta, speed))
                            .replyMarkup(smartInlineKeyboardService.getProcessingKeyboard(queueItem.getId(), locale))
                            .parseMode(ParseMode.HTML)
                            .build(),
                    true
            );
        }
    }
}
