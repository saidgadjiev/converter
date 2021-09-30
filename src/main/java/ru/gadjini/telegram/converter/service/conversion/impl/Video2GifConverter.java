package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.AnimationResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class Video2GifConverter extends BaseAny2AnyConverter {

    private static final String TAG = "v2gif";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(Format.GIF)
    );

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

    public SmartTempFile doConvert2Gif(String fileId, ConversionQueueItem fileQueueItem) {
        return doConvert2Gif(fileId, fileQueueItem, new AtomicReference<>(), false);
    }

    public SmartTempFile doConvert2Gif(String fileId, ConversionQueueItem fileQueueItem,
                                       AtomicReference<FFprobeDevice.WHD> whdAtomicReference, boolean withProgress) {
        SmartTempFile in = fileQueueItem.getDownloadedFileOrThrow(fileId);

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileId, TAG, Format.GIF.getExt());
        try {
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
            commandBuilder.hideBanner().quite().input(in.getAbsolutePath());

            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(in.getAbsolutePath());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(in.getAbsolutePath(), videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
            whdAtomicReference.set(whd);
            if (whd.getDuration() == null || whd.getDuration() > 300) {
                throw new UserException(localisationService.getMessage(
                        ConverterMessagesProperties.MESSAGE_VIDEO_2_GIF_MAX_LENGTH, userService.getLocaleOrDefault(fileQueueItem.getUserId())
                ));
            }
            long width = whd.getWidth() == null ? -1 : whd.getWidth();
            long height = whd.getHeight() == null ? 320 : whd.getHeight();
            commandBuilder.vf("fps=10,scale=" + width + ":" + height + ":flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse");
            commandBuilder.out(result.getAbsolutePath());

            if (withProgress) {
                fFmpegDevice.execute(commandBuilder.build(), callbackHandlerFactory.createCallback(fileQueueItem,
                        whd.getDuration(), userService.getLocaleOrDefault(fileQueueItem.getUserId())));
            } else {
                fFmpegDevice.execute(commandBuilder.build());
            }

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
        AtomicReference<FFprobeDevice.WHD> whdAtomicReference = new AtomicReference<>();
        SmartTempFile result = doConvert2Gif(fileQueueItem.getFirstFileId(), fileQueueItem, whdAtomicReference, true);
        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.GIF.getExt());

        return new AnimationResult(fileName, result, Format.GIF, downloadThumb(fileQueueItem), whdAtomicReference.get().getWidth(),
                whdAtomicReference.get().getHeight(), whdAtomicReference.get().getDuration(), false);
    }
}
