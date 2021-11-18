package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermark;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkType;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderChain;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler;
import ru.gadjini.telegram.converter.service.conversion.result.VideoResultBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChain;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChainFactory;
import ru.gadjini.telegram.converter.service.watermark.video.VideoWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileGarbageCollector;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileGarbageCollector.GarbageFileCollection;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;

@Component
public class FFmpegVideoWatermarkAdder extends BaseAny2AnyConverter {

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(Format.WATERMARK)
    );

    private static final String TAG = "vmark";

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private VideoWatermarkService videoWatermarkService;

    private FFprobeDevice fFprobeDevice;

    private FFmpegDevice fFmpegDevice;

    private LocalisationService localisationService;

    private UserService userService;

    private TempFileGarbageCollector tempFileGarbageCollector;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private FFmpegConversionContextPreparerChain conversionContextPreparer;

    private FFmpegCommandBuilderChain commandBuilderChain;

    private VideoResultBuilder videoResultBuilder;

    @Autowired
    public FFmpegVideoWatermarkAdder(FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                                     VideoWatermarkService videoWatermarkService,
                                     FFprobeDevice fFprobeDevice,
                                     FFmpegDevice fFmpegDevice,
                                     LocalisationService localisationService, UserService userService,
                                     TempFileGarbageCollector tempFileGarbageCollector,
                                     FFmpegProgressCallbackHandlerFactory callbackHandlerFactory,
                                     FFmpegConversionContextPreparerChainFactory chainFactory,
                                     FFmpegCommandBuilderFactory commandBuilderChainFactory,
                                     VideoResultBuilder videoResultBuilder) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.videoWatermarkService = videoWatermarkService;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
        this.localisationService = localisationService;
        this.userService = userService;
        this.tempFileGarbageCollector = tempFileGarbageCollector;
        this.callbackHandlerFactory = callbackHandlerFactory;
        this.videoResultBuilder = videoResultBuilder;

        this.commandBuilderChain = commandBuilderChainFactory.input();
        commandBuilderChain.setNext(commandBuilderChainFactory.videoWatermarkInput())
                .setNext(commandBuilderChainFactory.videoConversion())
                .setNext(commandBuilderChainFactory.videoWatermark())
                .setNext(commandBuilderChainFactory.audioInVideoConversion())
                .setNext(commandBuilderChainFactory.subtitlesConversion())
                .setNext(commandBuilderChainFactory.webmQuality())
                .setNext(commandBuilderChainFactory.fastVideoConversionAndDefaultOptions())
                .setNext(commandBuilderChainFactory.output());

        this.conversionContextPreparer = chainFactory.videoConversionContextPreparer();
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        int count = super.createDownloadsWithThumb(conversionQueueItem);
        VideoWatermark watermark = videoWatermarkService.getWatermark(conversionQueueItem.getUserId());
        if (watermark.getImage() != null) {
            fileDownloadService().createDownload(watermark.getImage(), conversionQueueItem.getId(),
                    conversionQueueItem.getUserId(), null);
        }

        return count + 1;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile video = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        GarbageFileCollection garbageFileCollection = tempFileGarbageCollector.getNewCollection();
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD,
                fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            VideoWatermark watermark = videoWatermarkService.getWatermark(fileQueueItem.getUserId());
            validateWatermarkFile(fileQueueItem, watermark);

            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(video.getAbsolutePath());
            FFmpegConversionContext conversionContext = new FFmpegConversionContext()
                    .input(video)
                    .streams(allStreams)
                    .outputFormat(fileQueueItem.getFirstFileFormat())
                    .output(result)
                    .putExtra(FFmpegConversionContext.VIDEO_WATERMARK, watermark)
                    .putExtra(FFmpegConversionContext.QUEUE_ITEM, fileQueueItem)
                    .putExtra(FFmpegConversionContext.GARBAGE_FILE_COLLECTOR, garbageFileCollection);
            conversionContextPreparer.prepare(conversionContext);

            FFmpegCommand command = new FFmpegCommand();
            commandBuilderChain.prepareCommand(command, conversionContext);

            FFprobeDevice.WHD wdh = fFprobeDevice.getWHD(video.getAbsolutePath(), 0);
            FFmpegProgressCallbackHandler callback = callbackHandlerFactory.createCallback(fileQueueItem, wdh.getDuration(),
                    userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            fFmpegDevice.execute(command.toCmd(), callback);

            return videoResultBuilder.build(fileQueueItem, fileQueueItem.getFirstFileFormat(), result);
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        } finally {
            garbageFileCollection.delete();
        }
    }

    private void validateWatermarkFile(ConversionQueueItem queueItem, VideoWatermark watermark) throws InterruptedException {
        if (watermark.getWatermarkType() == VideoWatermarkType.VIDEO) {
            SmartTempFile watermarkFile = queueItem.getDownloadedFileOrThrow(watermark.getImage().getFileId());

            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(watermarkFile.getAbsolutePath());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(watermarkFile.getAbsolutePath(), fFmpegVideoHelper.getFirstVideoStreamIndex(allStreams));
            if (whd.getDuration() == null || whd.getDuration() > 300) {
                throw new UserException(localisationService.getMessage(
                        ConverterMessagesProperties.MESSAGE_VIDEO_2_GIF_MAX_LENGTH, new Object[]{
                                whd.getDuration()
                        }, userService.getLocaleOrDefault(queueItem.getUserId())
                ));
            }
        }
    }
}
