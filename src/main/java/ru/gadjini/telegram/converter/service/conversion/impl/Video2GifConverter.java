package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.conversion.api.result.AnimationResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.common.TgConstants;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class Video2GifConverter extends BaseAny2AnyConverter {

    private static final String TAG = "v2gif";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(Format.GIF)
    );

    private static final int MAX_HEIGHT = 480;

    private FFprobeDevice fFprobeDevice;

    private LocalisationService localisationService;

    private FFmpegDevice fFmpegDevice;

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    private UserService userService;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    @Autowired
    public Video2GifConverter(FFprobeDevice fFprobeDevice,
                              LocalisationService localisationService, FFmpegDevice fFmpegDevice,
                              FFmpegVideoStreamConversionHelper videoStreamConversionHelper,
                              UserService userService, FFmpegProgressCallbackHandlerFactory callbackHandlerFactory) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.localisationService = localisationService;
        this.fFmpegDevice = fFmpegDevice;
        this.videoStreamConversionHelper = videoStreamConversionHelper;
        this.userService = userService;
        this.callbackHandlerFactory = callbackHandlerFactory;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    public SmartTempFile doConvert2Gif(String fileId, ConversionQueueItem fileQueueItem, int maxHeight) {
        SmartTempFile in = fileQueueItem.getDownloadedFileOrThrow(fileId);

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileId, TAG, Format.GIF.getExt());
        try {
            FFmpegCommand commandBuilder = new FFmpegCommand();
            commandBuilder.hideBanner().quite().input(in.getAbsolutePath());

            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreamsWithoutBitrate(in.getAbsolutePath());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(in.getAbsolutePath(), videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));

            long gifHeight = whd.getHeight() == null ? maxHeight : Math.min(whd.getHeight(), maxHeight);

            commandBuilder.vf("fps=5,scale=-2:" + gifHeight + ":flags=lanczos");
            commandBuilder.out(result.getAbsolutePath());

            fFmpegDevice.execute(commandBuilder.toCmd(), callbackHandlerFactory.createCallback(fileQueueItem,
                    whd.getDuration(), userService.getLocaleOrDefault(fileQueueItem.getUserId())));

            return result;
        } catch (UserException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getSize() > TgConstants.MAX_GIF_SIZE) {
            throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_2_GIF_MAX_SIZE,
                    userService.getLocaleOrDefault(fileQueueItem.getUserId())));
        }
        SmartTempFile in = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, Format.MP4.getExt());
        try {
            FFmpegCommand commandBuilder = new FFmpegCommand().hideBanner().quite()
                    .input(in.getAbsolutePath());

            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreamsWithoutBitrate(in.getAbsolutePath());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(in.getAbsolutePath(), videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
            commandBuilder.mapVideo(videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));

            String scaleFilter = null;
            if (!Objects.equals(whd.getHeight(), MAX_HEIGHT)) {
                long srcHeight = whd.getHeight() == null ? MAX_HEIGHT : whd.getHeight();
                scaleFilter = "scale=-2:" + Math.min(srcHeight, MAX_HEIGHT);
            }

            if (StringUtils.isNotBlank(scaleFilter)) {
                commandBuilder.vf(scaleFilter);
            } else {
                commandBuilder.copyVideo();
            }
            commandBuilder.out(result.getAbsolutePath());
            fFmpegDevice.execute(commandBuilder.toCmd());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.MP4.getExt());
            return new AnimationResult(fileName, result, Format.MP4, downloadThumb(fileQueueItem), whd.getWidth(),
                    whd.getHeight(), whd.getDuration(), true);
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}
