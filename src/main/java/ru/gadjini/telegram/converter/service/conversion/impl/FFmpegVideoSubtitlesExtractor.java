package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResults;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.filter;

@Component
public class FFmpegVideoSubtitlesExtractor extends BaseAny2AnyConverter {

    private static final String TAG = "vesubs";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(filter(FormatCategory.VIDEO), filter(FormatCategory.SUBTITLES));
    }};

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegVideoHelper fFmpegVideoHelper;

    @Autowired
    public FFmpegVideoSubtitlesExtractor(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                         FFmpegVideoHelper fFmpegVideoHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        try {
            fFmpegVideoHelper.validateVideoIntegrity(file);
            List<FFprobeDevice.Stream> subtitleStreams = fFprobeDevice.getSubtitleStreams(file.getAbsolutePath());
            ConvertResults convertResults = new ConvertResults();
            for (int streamIndex = 0; streamIndex < subtitleStreams.size(); streamIndex++) {
                FFprobeDevice.Stream subtitleStream = subtitleStreams.get(streamIndex);
                SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                        fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

                try {
                    FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().mapSubtitles(streamIndex);
                    fFmpegDevice.convert(file.getAbsolutePath(), result.getAbsolutePath(), commandBuilder.build());

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(),
                            String.valueOf(streamIndex), fileQueueItem.getTargetFormat().getExt());
                    convertResults.addResult(new FileResult(fileName, result, subtitleStream.getLanguage()));
                } catch (Exception e) {
                    tempFileService().delete(result);
                    throw e;
                }
            }

            return convertResults;
        } catch (CorruptedVideoException e) {
            throw e;
        } catch (Exception e) {
            throw new ConvertException(e);
        }
    }
}
