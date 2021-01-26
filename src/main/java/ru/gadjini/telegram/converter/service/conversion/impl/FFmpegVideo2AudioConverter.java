package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.*;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.converter.utils.FFmpegHelper;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

/**
 * MP4 -> WEBM very slow
 * WEBM -> MP4 very slow
 */
@Component
public class FFmpegVideo2AudioConverter extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegVideo2AudioConverter.class);

    private static final String TAG = "ffmpegvideo";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(MP4, _3GP, AVI, TS, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV),
                List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, OPUS, SPX, M4A, VOICE, RA));
    }};

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public FFmpegVideo2AudioConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        List<FFprobeDevice.Stream> audioStreams = fFprobeDevice.getAudioStreams(file.getAbsolutePath());
        ConvertResults convertResults = new ConvertResults();
        for (FFprobeDevice.Stream audioStream : audioStreams) {
            SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

            try {
                String[] options = Stream.concat(Stream.of(getExtractAudioOptions(audioStream.getIndex())), Stream.of(FFmpegHelper.getAudioOptions(fileQueueItem.getTargetFormat()))).toArray(String[]::new);
                fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), options);

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), String.valueOf(audioStream.getIndex()), fileQueueItem.getTargetFormat().getExt());

                if (fileQueueItem.getTargetFormat().canBeSentAsAudio()
                        || fileQueueItem.getTargetFormat() == VOICE) {
                    long durationInSeconds = 0;
                    try {
                        durationInSeconds = fFprobeDevice.getDurationInSeconds(out.getAbsolutePath());
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                    if (fileQueueItem.getTargetFormat().equals(VOICE)) {
                        convertResults.addResult(new VoiceResult(fileName, out, (int) durationInSeconds, audioStream.getLanguage()));
                    } else {
                        convertResults.addResult(new AudioResult(fileName, out, null, null, downloadThumb(fileQueueItem), (int) durationInSeconds, audioStream.getLanguage()));
                    }
                } else {
                    convertResults.addResult(new FileResult(fileName, out, downloadThumb(fileQueueItem), audioStream.getLanguage()));
                }
            } catch (Exception e) {
                out.smartDelete();
                throw e;
            }
        }

        return convertResults;

    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }

    private String[] getExtractAudioOptions(int streamIndex) {
        return new String[]{
                "-map", "0:" + streamIndex
        };
    }
}
