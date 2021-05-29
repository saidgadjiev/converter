package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoNoteResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.VIDEO_NOTE;

@Component
@SuppressWarnings("CPD-START")
public class VideoNoteMaker extends BaseAny2AnyConverter {

    private static final String TAG = "vnote";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(Format.filter(FormatCategory.VIDEO).stream().filter(Format::supportsStreaming).collect(Collectors.toList()), List.of(VIDEO_NOTE));
    }};

    private FFprobeDevice fFprobeDevice;

    public VideoNoteMaker(FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            Files.move(file.toPath(), result.toPath(), StandardCopyOption.REPLACE_EXISTING);
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(result.getAbsolutePath());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), FFmpegVideoConversionHelper.getFirstVideoStreamIndex(allStreams));

            return new VideoNoteResult(fileQueueItem.getFirstFileName(), result, whd.getDuration(), fileQueueItem.getFirstFileFormat());
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}
