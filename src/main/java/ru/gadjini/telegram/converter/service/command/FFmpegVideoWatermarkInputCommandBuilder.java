package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.command.bot.watermark.video.state.WatermarkImageSizeState;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermark;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkType;
import ru.gadjini.telegram.converter.service.conversion.impl.Tgs2GifConverter;
import ru.gadjini.telegram.converter.service.conversion.impl.Video2GifConverter;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileGarbageCollector.GarbageFileCollection;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.Set;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.TGS;

public class FFmpegVideoWatermarkInputCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private Tgs2GifConverter tgs2GifConverter;

    private Video2GifConverter video2GifConverter;

    private LocalisationService localisationService;

    private UserService userService;

    public FFmpegVideoWatermarkInputCommandBuilder(Tgs2GifConverter tgs2GifConverter, Video2GifConverter video2GifConverter,
                                                   LocalisationService localisationService, UserService userService) {
        this.tgs2GifConverter = tgs2GifConverter;
        this.video2GifConverter = video2GifConverter;
        this.localisationService = localisationService;
        this.userService = userService;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        VideoWatermark watermark = conversionContext.getExtra(FFmpegConversionContext.VIDEO_WATERMARK);
        if (isNeedLoop(watermark)) {
            command.ignoreLoop();
        }

        if (watermark.getWatermarkType() != VideoWatermarkType.TEXT) {
            command.useFilterComplex();
            GarbageFileCollection garbageFileCollection = conversionContext.getExtra(FFmpegConversionContext.GARBAGE_FILE_COLLECTOR);
            ConversionQueueItem fileQueueItem = conversionContext.getExtra(FFmpegConversionContext.QUEUE_ITEM);
            SmartTempFile watermarkImage = prepareAndGetWatermarkFile(garbageFileCollection, fileQueueItem, watermark);
            command.input(watermarkImage.getAbsolutePath());
        }

        super.prepareCommand(command, conversionContext);
    }

    private SmartTempFile prepareAndGetWatermarkFile(GarbageFileCollection garbageFileCollection,
                                                     ConversionQueueItem fileQueueItem, VideoWatermark watermark) {
        switch (watermark.getWatermarkType()) {
            case IMAGE: {
                return fileQueueItem.getDownloadedFileOrThrow(watermark.getImage().getFileId());
            }
            case STICKER: {
                if (watermark.getImage().getFormat() == TGS) {
                    SmartTempFile file = tgs2GifConverter.doConvertToGiff(watermark.getImage().getFileId(), fileQueueItem);
                    garbageFileCollection.addFile(file);

                    return file;
                } else {
                    return fileQueueItem.getDownloadedFileOrThrow(watermark.getImage().getFileId());
                }
            }
            case VIDEO: {
                return convertVideo2Gif(garbageFileCollection, fileQueueItem, watermark);
            }
            case GIF: {
                if (watermark.getImage().getFormat().getCategory() == FormatCategory.VIDEO) {
                    return convertVideo2Gif(garbageFileCollection, fileQueueItem, watermark);
                } else {
                    return fileQueueItem.getDownloadedFileOrThrow(watermark.getImage().getFileId());
                }
            }
        }

        throw new UnsupportedOperationException("prepareAndGetWatermarkFile unsupported for " + watermark.getWatermarkType());
    }

    private SmartTempFile convertVideo2Gif(GarbageFileCollection garbageFileCollection,
                                           ConversionQueueItem fileQueueItem, VideoWatermark watermark) {
        try {
            SmartTempFile file = video2GifConverter.doConvert2Gif(watermark.getImage().getFileId(), fileQueueItem,
                    watermark.getImageHeight() == null ? WatermarkImageSizeState.MAX_HEIGHT : watermark.getImageHeight());
            garbageFileCollection.addFile(file);

            return file;
        } catch (UserException e) {
            throw new UserException(
                    localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_WATERMARK_MAX_LENGTH,
                            userService.getLocaleOrDefault(fileQueueItem.getUserId()))
            );
        }
    }

    private boolean isNeedLoop(VideoWatermark watermark) {
        return Set.of(VideoWatermarkType.GIF, VideoWatermarkType.VIDEO).contains(watermark.getWatermarkType())
                || watermark.getWatermarkType() == VideoWatermarkType.STICKER && watermark.getImage().getFormat() == TGS;
    }
}
