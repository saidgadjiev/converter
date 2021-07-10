package ru.gadjini.telegram.converter.service.conversion.impl;

import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.MessageResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.PROBE;

@Component
public class VideoProbber extends BaseAny2AnyConverter {

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(PROBE)
    );

    private FFprobeDevice fFprobeDevice;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private LocalisationService localisationService;

    private UserService userService;

    @Autowired
    public VideoProbber(FFprobeDevice fFprobeDevice, FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                        LocalisationService localisationService, UserService userService) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        try {
            fFmpegVideoHelper.validateVideoIntegrity(file);
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(file.getAbsolutePath(), 0, true);

            String text = localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_PROBE_RESULT,
                    new Object[]{fileQueueItem.getFirstFileFormat().getName(),
                            whd.getHeight() != null ? whd.getHeight() + "p" : "unknown",
                            whd.getHeight() != null ? whd.getWidth() + "x" + whd.getHeight() : "unknown",
                            length(whd.getDuration()),
                            MemoryUtils.humanReadableByteCount(fileQueueItem.getSize()),
                            allStreams.stream().filter(f -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(f.getCodecType())).count(),
                            allStreams.stream().filter(f -> FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE.equals(f.getCodecType())).count(),
                    },
                    userService.getLocaleOrDefault(fileQueueItem.getUserId()));

            return new MessageResult(SendMessage.builder()
                    .chatId(String.valueOf(fileQueueItem.getUserId()))
                    .text(text)
                    .parseMode(ParseMode.HTML)
                    .replyToMessageId(fileQueueItem.getReplyToMessageId())
                    .build(),
                    true
            );
        } catch (Throwable e) {
            throw new ConvertException(e);
        }
    }

    private String length(Long seconds) {
        if (seconds == null) {
            return "unknown";
        }
        return VideoCutter.PERIOD_FORMATTER.print(new Period(seconds * 1000L));
    }
}
