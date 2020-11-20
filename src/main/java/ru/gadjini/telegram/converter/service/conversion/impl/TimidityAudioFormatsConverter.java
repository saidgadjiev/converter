package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class TimidityAudioFormatsConverter extends BaseAny2AnyConverter {

    private static final String TAG = "timidityaudio";

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(MID), List.of(AAC, AMR, AIFF, FLAC, MP3, WMA, OPUS, SPX, M4A));
    }};

    private FFmpegDevice fFmpegDevice;

    private TempFileService fileService;

    private Timidity2WavFormatConverter wavFormatConverter;

    @Autowired
    public TimidityAudioFormatsConverter(FFmpegDevice fFmpegDevice, TempFileService fileService, Timidity2WavFormatConverter wavFormatConverter) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fileService = fileService;
        this.wavFormatConverter = wavFormatConverter;
    }

    @Override
    public ConvertResult convert(ConversionQueueItem fileQueueItem) {
        Format targetFormat = fileQueueItem.getTargetFormat();
        fileQueueItem.setTargetFormat(WAV);
        FileResult convertResult = (FileResult) wavFormatConverter.convert(fileQueueItem);
        SmartTempFile toDelete = convertResult.getSmartFile();

        try {
            SmartTempFile out = fileService.createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, targetFormat.getExt());
            try {
                fFmpegDevice.convert(convertResult.getFile().getAbsolutePath(), out.getAbsolutePath(), getOptions(targetFormat));

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), targetFormat.getExt());
                convertResult.setSmartFile(out);
                convertResult.setFileName(fileName);

                return convertResult;
            } catch (Throwable e) {
                out.smartDelete();
                throw e;
            }
        } finally {
            toDelete.smartDelete();
        }
    }

    private String[] getOptions(Format target) {
        if (target == AMR) {
            return new String[]{
                    "-ar", "8000", "-ac", "1"
            };
        }
        if (target == OGG) {
            return new String[]{
                    "-c:a", "libvorbis", "-q:a", "4"
            };
        }
        return new String[0];
    }
}
