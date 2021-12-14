package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoNoteResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.StreamsChecker;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.common.TgConstants;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileGarbageCollector;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.VIDEO_NOTE;

@Component
public class VideoNoteMaker extends BaseAny2AnyConverter {

    private static final String TAG = "vnote";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(Format.filter(FormatCategory.VIDEO), List.of(VIDEO_NOTE));
    }};

    private FFprobeDevice fFprobeDevice;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private LocalisationService localisationService;

    private UserService userService;

    private MakeVideoSquare makeVideoSquare;

    private StreamsChecker streamsChecker;

    private TempFileGarbageCollector tempFileGarbageCollector;

    @Autowired
    public VideoNoteMaker(FFprobeDevice fFprobeDevice, FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                          LocalisationService localisationService, UserService userService,
                          MakeVideoSquare makeVideoSquare, StreamsChecker streamsChecker,
                          TempFileGarbageCollector tempFileGarbageCollector) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.localisationService = localisationService;
        this.userService = userService;
        this.makeVideoSquare = makeVideoSquare;
        this.streamsChecker = streamsChecker;
        this.tempFileGarbageCollector = tempFileGarbageCollector;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        TempFileGarbageCollector.GarbageFileCollection garbageFileCollection = tempFileGarbageCollector.getNewCollection();
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            List<FFprobeDevice.FFProbeStream> streams = fFprobeDevice.getStreams(file.getAbsolutePath(), FormatCategory.VIDEO);

            Locale locale = userService.getLocaleOrDefault(fileQueueItem.getUserId());
            streamsChecker.checkVideoStreamsExistence(streams, locale);
            FFprobeDevice.WHD whd = fFmpegVideoHelper.getFirstVideoStream(streams).getWhd();

            if (!Objects.equals(whd.getHeight(), whd.getWidth())) {
                SmartTempFile squareResult = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                        fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
                garbageFileCollection.addFile(squareResult);

                int size = makeVideoSquare.doMakeSquare(file, squareResult, fileQueueItem, streams);
                file = squareResult;
                whd.setWidth(size);
                whd.setHeight(size);

                if (result.length() > TgConstants.VIDEO_NOTE_MAX_FILE_SIZE) {
                    throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INVALID_VIDEO_NOTE_CANDIDATE_DIMENSION,
                            new Object[]{whd.getWidth() + "x" + whd.getHeight()}, userService.getLocaleOrDefault(fileQueueItem.getUserId())))
                            .setReplyToMessageId(fileQueueItem.getReplyToMessageId());
                }
            }
            if (whd.getDuration() > TgConstants.VIDEO_NOTE_MAX_LENGTH) {
                throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INVALID_VIDEO_NOTE_LENGTH,
                        new Object[]{whd.getDuration()}, userService.getLocaleOrDefault(fileQueueItem.getUserId())))
                        .setReplyToMessageId(fileQueueItem.getReplyToMessageId());
            }

            Files.move(file.toPath(), result.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return new VideoNoteResult(fileQueueItem.getFirstFileName(), result, whd.getDuration(), fileQueueItem.getFirstFileFormat());
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        } finally {
            garbageFileCollection.delete();
        }
    }

    private void tryNonSquareVideoConversion(ConversionQueueItem fileQueueItem) {

    }

    private void trySquareVideoConversion(ConversionQueueItem fileQueueItem) {

    }
}
