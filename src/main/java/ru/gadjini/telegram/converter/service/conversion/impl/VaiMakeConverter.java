package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class VaiMakeConverter extends BaseAny2AnyConverter {

    private static final String TAG = "vaimake";

    public static final Format OUTPUT_FORMAT = Format.MP4;

    public static final int AUDIO_FILE_INDEX = 0;

    public static final int IMAGE_FILE_INDEX = 1;

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.IMAGEAUDIO), List.of(Format.VMAKE)
    );

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private UserService userService;

    @Autowired
    public VaiMakeConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                            FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper,
                            FFmpegProgressCallbackHandlerFactory callbackHandlerFactory, UserService userService) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.videoAudioConversionHelper = videoAudioConversionHelper;
        this.callbackHandlerFactory = callbackHandlerFactory;
        this.userService = userService;
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem conversionQueueItem) {
        SmartTempFile downloadedAudio = conversionQueueItem.getDownloadedFiles().get(AUDIO_FILE_INDEX);
        SmartTempFile downloadedImage = conversionQueueItem.getDownloadedFiles().get(IMAGE_FILE_INDEX);

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, conversionQueueItem.getUserId(),
                conversionQueueItem.getFirstFileId(), TAG, OUTPUT_FORMAT.getExt());

        try {
            long durationInSeconds = fFprobeDevice.getDurationInSeconds(downloadedAudio.getAbsolutePath());
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder()
                    .loop(1).hideBanner().quite().framerate("1").input(downloadedImage.getAbsolutePath())
                    .input(downloadedAudio.getAbsolutePath()).videoCodec(FFmpegCommandBuilder.H264_CODEC)
                    .filterVideo(FFmpegCommandBuilder.EVEN_SCALE)
                    .tune(FFmpegCommandBuilder.TUNE_STILLIMAGE).t(durationInSeconds);

            List<FFprobeDevice.FFProbeStream> audioStreams = fFprobeDevice.getAllStreams(downloadedAudio.getAbsolutePath());
            videoAudioConversionHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, audioStreams, false);
            commandBuilder.defaultOptions().out(result.getAbsolutePath());

            FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler callback = callbackHandlerFactory.createCallback(conversionQueueItem, durationInSeconds,
                    userService.getLocaleOrDefault(conversionQueueItem.getUserId()));
            fFmpegDevice.execute(commandBuilder.buildFullCommand(), callback);

            String fileName = Any2AnyFileNameUtils.getFileName(conversionQueueItem.getFiles().get(AUDIO_FILE_INDEX).getFileName(),
                    OUTPUT_FORMAT.getExt());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
            return new VideoResult(fileName, result, OUTPUT_FORMAT, downloadThumb(conversionQueueItem),
                    whd.getWidth(), whd.getHeight(), whd.getDuration(), OUTPUT_FORMAT.supportsStreaming());
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}
