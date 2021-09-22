package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermark;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkPosition;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkType;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.property.FontProperties;
import ru.gadjini.telegram.converter.service.caption.CaptionGenerator;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandler;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.watermark.video.VideoWatermarkService;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileGarbageCollector;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileGarbageCollector.GarbageFileCollection;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.TGS;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WEBM;

@Component
public class FFmpegVideoWatermarkAdder extends BaseAny2AnyConverter {

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(Format.WATERMARK)
    );

    private static final String TAG = "vmark";

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private VideoWatermarkService videoWatermarkService;

    private FFprobeDevice fFprobeDevice;

    private FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper;

    private FFmpegDevice fFmpegDevice;

    private FFmpegSubtitlesStreamConversionHelper subtitlesHelper;

    private FontProperties fontProperties;

    private CaptionGenerator captionGenerator;

    private LocalisationService localisationService;

    private Tgs2GifConverter tgs2GifConverter;

    private Video2GifConverter video2GifConverter;

    private UserService userService;

    private TempFileGarbageCollector tempFileGarbageCollector;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    @Autowired
    public FFmpegVideoWatermarkAdder(FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                                     VideoWatermarkService videoWatermarkService,
                                     FFprobeDevice fFprobeDevice, FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper,
                                     FFmpegDevice fFmpegDevice, FFmpegSubtitlesStreamConversionHelper subtitlesHelper,
                                     FontProperties fontProperties, CaptionGenerator captionGenerator,
                                     LocalisationService localisationService, Tgs2GifConverter tgs2GifConverter,
                                     Video2GifConverter video2GifConverter, UserService userService,
                                     TempFileGarbageCollector tempFileGarbageCollector,
                                     FFmpegProgressCallbackHandlerFactory callbackHandlerFactory) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.videoWatermarkService = videoWatermarkService;
        this.fFprobeDevice = fFprobeDevice;
        this.videoAudioConversionHelper = videoAudioConversionHelper;
        this.fFmpegDevice = fFmpegDevice;
        this.subtitlesHelper = subtitlesHelper;
        this.fontProperties = fontProperties;
        this.captionGenerator = captionGenerator;
        this.localisationService = localisationService;
        this.tgs2GifConverter = tgs2GifConverter;
        this.video2GifConverter = video2GifConverter;
        this.userService = userService;
        this.tempFileGarbageCollector = tempFileGarbageCollector;
        this.callbackHandlerFactory = callbackHandlerFactory;
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
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().hideBanner().quite()
                    .input(video.getAbsolutePath());

            if (Set.of(VideoWatermarkType.GIF, VideoWatermarkType.VIDEO).contains(watermark.getWatermarkType())
                    || watermark.getImage().getFormat() == TGS) {
                commandBuilder.ignoreLoop();
            }

            if (watermark.getWatermarkType() != VideoWatermarkType.TEXT) {
                commandBuilder.useFilterComplex(true);
                SmartTempFile watermarkImage = prepareAndGetWatermarkFile(garbageFileCollection, fileQueueItem, watermark);
                commandBuilder.input(watermarkImage.getAbsolutePath());
            }

            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(video.getAbsolutePath());
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                fFmpegVideoHelper.convertVideoCodecsForTelegramVideo(commandBuilder, allStreams,
                        fileQueueItem.getFirstFileFormat(), fileQueueItem.getSize());
            } else {
                fFmpegVideoHelper.convertVideoCodecs(commandBuilder, allStreams, fileQueueItem.getFirstFileFormat(),
                        result, fileQueueItem.getSize());
            }

            if (watermark.getWatermarkType() == VideoWatermarkType.TEXT) {
                commandBuilder.filterVideo(0, createTextWatermarkFilter(watermark));
            } else {
                if (commandBuilder.getComplexFilters().stream().anyMatch(f -> f.contains("[sv]"))) {
                    commandBuilder.complexFilter(createWatermarkFileFilter("sv", watermark));
                } else {
                    commandBuilder.complexFilter(createWatermarkFileFilter("0", watermark));
                }
            }
            commandBuilder.complexFilters();

            fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, fileQueueItem.getFirstFileFormat());
            FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                videoAudioConversionHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams);
            } else {
                videoAudioConversionHelper.copyOrConvertAudioCodecs(baseCommand, commandBuilder, allStreams, result, fileQueueItem.getFirstFileFormat());
            }
            subtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder, allStreams, result, fileQueueItem.getFirstFileFormat());
            if (WEBM.equals(fileQueueItem.getFirstFileFormat())) {
                commandBuilder.vp8QualityOptions();
            }
            commandBuilder.fastConversion().defaultOptions();
            commandBuilder.out(result.getAbsolutePath());

            FFprobeDevice.WHD wdh = fFprobeDevice.getWHD(video.getAbsolutePath(), 0);
            FFmpegProgressCallbackHandler callback = callbackHandlerFactory.createCallback(fileQueueItem, wdh.getDuration(),
                    userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            fFmpegDevice.execute(commandBuilder.buildFullCommand(), callback);

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            String caption = captionGenerator.generate(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getSource());
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), wdh.getWidth(), wdh.getHeight(),
                        wdh.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming(), caption);
            } else {
                return new FileResult(fileName, result, downloadThumb(fileQueueItem), caption);
            }
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
            SmartTempFile file = video2GifConverter.doConvert2Gif(watermark.getImage().getFileId(), fileQueueItem);
            garbageFileCollection.addFile(file);

            return file;
        } catch (UserException e) {
            throw new UserException(
                    localisationService.getMessage(ConverterMessagesProperties.MESSAGE_VIDEO_WATERMARK_MAX_LENGTH,
                            userService.getLocaleOrDefault(fileQueueItem.getUserId()))
            );
        }
    }

    private String createTextWatermarkFilter(VideoWatermark videoWatermark) {
        StringBuilder filter = new StringBuilder();

        filter.append("drawtext=text='").append(videoWatermark.getText()).append("':")
                .append(getTextXY(videoWatermark.getWatermarkPosition())).append(":")
                .append("fontfile=").append(getFontPath("tahoma")).append(":")
                .append("fontsize=").append(videoWatermark.getFontSize() == null ? "(h/30)" : videoWatermark.getFontSize()).append(":")
                .append("fontcolor=").append(videoWatermark.getColor().name().toLowerCase());

        return filter.toString();
    }

    private String createWatermarkFileFilter(String videoComplexFilterInLink, VideoWatermark videoWatermark) {
        switch (videoWatermark.getWatermarkType()) {
            case IMAGE:
                return createImageWatermarkFilter(videoComplexFilterInLink, videoWatermark);
            case STICKER:
                if (videoWatermark.getImage().getFormat() == TGS) {
                    return createGifWatermarkFilter(videoComplexFilterInLink, videoWatermark);
                } else {
                    return createImageWatermarkFilter(videoComplexFilterInLink, videoWatermark);
                }
            case VIDEO:
            case GIF:
                return createGifWatermarkFilter(videoComplexFilterInLink, videoWatermark);
        }

        throw new UnsupportedOperationException("createWatermarkFileFilter unsupported for " + videoWatermark.getWatermarkType());
    }

    private String createGifWatermarkFilter(String videoComplexFilterInLink, VideoWatermark videoWatermark) {
        StringBuilder filter = new StringBuilder();

        filter.append("[1]scale=-2:").append(videoWatermark.getImageHeight() == null ? "ih*0.2" : videoWatermark.getImageHeight())
                .append("[wm];[wm]lut=a=val*").append(videoWatermark.getTransparency()).append("[a];[").append(videoComplexFilterInLink)
                .append("][a]overlay=").append(getImageXY(videoWatermark.getWatermarkPosition())).append(":shortest=1");

        return filter.toString();
    }

    private String createImageWatermarkFilter(String videoComplexFilterInLink, VideoWatermark videoWatermark) {
        StringBuilder filter = new StringBuilder();

        filter.append("[1]scale=-2:").append(videoWatermark.getImageHeight() == null ? "ih*0.2" : videoWatermark.getImageHeight())
                .append("[wm];[wm]lut=a=val*").append(videoWatermark.getTransparency()).append("[a];[").append(videoComplexFilterInLink)
                .append("][a]overlay=").append(getImageXY(videoWatermark.getWatermarkPosition()));

        return filter.toString();
    }

    private String getTextXY(VideoWatermarkPosition position) {
        switch (position) {
            case TOP_LEFT:
                return "x=10:y=10";
            case TOP_MIDDLE:
                return "x=(w-text_w)/2:y=10";
            case TOP_RIGHT:
                return "x=w-tw-10:y=10";
            case MIDDLE_LEFT:
                return "x=10:y=(h-text_h)/2";
            case MIDDLE:
                return "x=(w-text_w)/2:y=(h-text_h)/2";
            case MIDDLE_RIGHT:
                return "x=w-tw-10:y=(h-text_h)/2";
            case BOTTOM_LEFT:
                return "x=10:y=h-th-10";
            case BOTTOM_MIDDLE:
                return "x=(w-text_w)/2:y=h-th-10";
            case BOTTOM_RIGHT:
                return "x=w-tw-10:y=h-th-10";
        }

        throw new IllegalArgumentException("Unknown position " + position.name());
    }

    private String getImageXY(VideoWatermarkPosition position) {
        switch (position) {
            case TOP_LEFT:
                return "x=10:y=10";
            case TOP_MIDDLE:
                return "x=(main_w-overlay_w)/2:y=10";
            case TOP_RIGHT:
                return "x=main_w-overlay_w-10:y=10";
            case MIDDLE_LEFT:
                return "x=10:y=(main_h-overlay_h)/2";
            case MIDDLE:
                return "x=(main_w-overlay_w)/2:y=(main_h-overlay_h)/2";
            case MIDDLE_RIGHT:
                return "x=main_w-overlay_w-10:y=(main_h-overlay_h)/2";
            case BOTTOM_LEFT:
                return "x=10:y=main_h-overlay_h-10";
            case BOTTOM_MIDDLE:
                return "x=(main_w-overlay_w)/2:y=main_h-overlay_h-10";
            case BOTTOM_RIGHT:
                return "x=main_w-overlay_w-10:y=main_h-overlay_h-10";
        }

        throw new IllegalArgumentException("Unknown position " + position.name());
    }

    private String getFontPath(String fontName) {
        return fontProperties.getPath() + File.separator + fontName + ".ttf";
    }
}
