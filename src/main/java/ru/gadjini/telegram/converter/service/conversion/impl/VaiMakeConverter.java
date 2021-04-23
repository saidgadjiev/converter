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
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class VaiMakeConverter extends BaseAny2AnyConverter {

    private static final String TAG = "vaimake";

    private static final Format OUTPUT_FORMAT = Format.MP4;

    public static final int AUDIO_FILE_INDEX = 0;

    public static final int IMAGE_FILE_INDEX = 1;

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.IMAGEAUDIO), List.of(Format.VMAKE)
    );

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public VaiMakeConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem conversionQueueItem) {
        conversionQueueItem.setTargetFormat(OUTPUT_FORMAT);
        SmartTempFile downloadedAudio = conversionQueueItem.getDownloadedFiles().get(AUDIO_FILE_INDEX);
        SmartTempFile downloadedImage = conversionQueueItem.getDownloadedFiles().get(IMAGE_FILE_INDEX);

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, conversionQueueItem.getUserId(),
                conversionQueueItem.getFirstFileId(), TAG, OUTPUT_FORMAT.getExt());

        try {
            long durationInSeconds = fFprobeDevice.getDurationInSeconds(downloadedAudio.getAbsolutePath());
            FFmpegCommandBuilder baseVaiMakeCommand = new FFmpegCommandBuilder();
            baseVaiMakeCommand.loop(1).quite().framerate("1").input(downloadedImage.getAbsolutePath())
                    .input(downloadedAudio.getAbsolutePath()).videoCodec(FFmpegCommandBuilder.H264_CODEC)
                    .tune(FFmpegCommandBuilder.TUNE_STILLIMAGE).pixFmt(FFmpegCommandBuilder.YUV_420_P).shortest().t(durationInSeconds);

            try {
                FFmpegCommandBuilder withCopyAudio = new FFmpegCommandBuilder(baseVaiMakeCommand);
                withCopyAudio.copyAudio().out(result.getAbsolutePath());

                fFmpegDevice.execute(withCopyAudio.buildFullCommand());
            } catch (ProcessException e) {
                FFmpegCommandBuilder withAudioConvert = new FFmpegCommandBuilder(baseVaiMakeCommand);
                if (conversionQueueItem.getFiles().get(AUDIO_FILE_INDEX).getFormat() == Format.MP3) {
                    //Keep mp3 codec
                    withAudioConvert.audioCodec(FFmpegCommandBuilder.LIBMP3LAME);
                }
                withAudioConvert.out(result.getAbsolutePath());

                fFmpegDevice.execute(withAudioConvert.buildFullCommand());
            }

            String fileName = Any2AnyFileNameUtils.getFileName(conversionQueueItem.getFiles().get(AUDIO_FILE_INDEX).getFileName(),
                    OUTPUT_FORMAT.getExt());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
            return new VideoResult(fileName, result, OUTPUT_FORMAT, whd.getWidth(), whd.getHeight(), whd.getDuration(), true);
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}
