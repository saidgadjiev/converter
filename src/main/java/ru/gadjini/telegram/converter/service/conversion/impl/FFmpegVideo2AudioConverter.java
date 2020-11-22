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
        put(List.of(MP4, _3GP, AVI, FLV, M4V, MKV, MOV, MPEG, MPG, MTS, VOB, WMV),
                List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, OPUS, SPX, M4A, VOICE));
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
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        try {
            List<FFprobeDevice.AudioStream> audioStreams = fFprobeDevice.getAudioStreams(file.getAbsolutePath());
            ConvertResults convertResults = new ConvertResults();
            for (FFprobeDevice.AudioStream audioStream : audioStreams) {
                int streamIndex = audioStream.getIndex() - 1;
                SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getTargetFormat().getExt());

                try {
                    String[] options = Stream.concat(Stream.of(getExtractAudioOptions(streamIndex)), Stream.of(FFmpegHelper.getAudioOptions(fileQueueItem.getTargetFormat()))).toArray(String[]::new);
                    fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), options);

                    String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), String.valueOf(streamIndex), fileQueueItem.getTargetFormat().getExt());

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
        } finally {
            file.smartDelete();
        }
    }

    private String[] getExtractAudioOptions(int streamIndex) {
        return new String[]{
                "-map", "0:a:" + streamIndex
        };
    }
}