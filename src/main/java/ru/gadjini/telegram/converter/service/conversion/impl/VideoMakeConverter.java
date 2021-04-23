package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class VideoMakeConverter extends BaseAny2AnyConverter {

    private static final String TAG = "vmake";

    private static final Format OUTPUT_FORMAT = Format.MP4;

    private static final Format IMAGE_FORMAT = Format.PNG;

    private static final Format AUDIO_FORMAT = Format.MP3;

    public static final int AUDIO_FILE_INDEX = 1;

    public static final int IMAGE_FILE_INDEX = 0;

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.IMAGEAUDIO), List.of(Format.VMAKE)
    );

    private FFmpegAudioFormatsConverter audioFormatsConverter;

    private Image2AnyConverter image2AnyConverter;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public VideoMakeConverter(FFmpegAudioFormatsConverter audioFormatsConverter,
                              Image2AnyConverter image2AnyConverter, FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.audioFormatsConverter = audioFormatsConverter;
        this.image2AnyConverter = image2AnyConverter;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem conversionQueueItem) {
        conversionQueueItem.setTargetFormat(OUTPUT_FORMAT);
        SmartTempFile downloadedAudio = conversionQueueItem.getDownloadedFiles().get(AUDIO_FILE_INDEX);
        SmartTempFile audio = downloadedAudio;
        SmartTempFile downloadedImage = conversionQueueItem.getDownloadedFiles().get(IMAGE_FILE_INDEX);
        SmartTempFile image = downloadedImage;
        try {
            if (conversionQueueItem.getFiles().get(AUDIO_FILE_INDEX).getFormat() != Format.MP3) {
                audio = tempFileService().createTempFile(FileTarget.UPLOAD, conversionQueueItem.getUserId(),
                        conversionQueueItem.getFirstFileId(), TAG, AUDIO_FORMAT.getExt());
                audioFormatsConverter.doConvertAudio(downloadedAudio, audio, AUDIO_FORMAT);
            }
            if (conversionQueueItem.getFiles().get(IMAGE_FILE_INDEX).getFormat() != Format.PNG) {
                image = tempFileService().createTempFile(FileTarget.UPLOAD, conversionQueueItem.getUserId(),
                        conversionQueueItem.getFirstFileId(), TAG, IMAGE_FORMAT.getExt());
                image2AnyConverter.doConvert(downloadedImage, image, IMAGE_FORMAT);
            }
            SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, conversionQueueItem.getUserId(),
                    conversionQueueItem.getFirstFileId(), TAG, OUTPUT_FORMAT.getExt());

            try {
                FFmpegCommandBuilder vmakeCommandBuilder = new FFmpegCommandBuilder();
                vmakeCommandBuilder.loop(1).quite().r("1").input(image.getAbsolutePath())
                        .input(audio.getAbsolutePath()).videoCodec(FFmpegCommandBuilder.H264_CODEC)
                        .tune(FFmpegCommandBuilder.TUNE_STILLIMAGE).audioCodec(FFmpegCommandBuilder.LIBMP3LAME)
                        .pixFmt(FFmpegCommandBuilder.YUV_420_P).shortest().out(result.getAbsolutePath());

                fFmpegDevice.execute(vmakeCommandBuilder.buildFullCommand());

                String fileName = Any2AnyFileNameUtils.getFileName(conversionQueueItem.getFiles().get(AUDIO_FILE_INDEX).getFileName(),
                        OUTPUT_FORMAT.getExt());
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
                return new VideoResult(fileName, result, OUTPUT_FORMAT, whd.getWidth(), whd.getHeight(), whd.getDuration(), true);
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw new ConvertException(e);
            }
        } catch (InterruptedException e) {
            throw new ConvertException(e);
        } finally {
            tempFileService().delete(audio);
            tempFileService().delete(image);
        }
    }
}
