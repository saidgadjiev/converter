package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.watermark.video.VideoWatermarkService;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.io.File;
import java.util.List;
import java.util.Map;

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

    @Autowired
    public FFmpegVideoWatermarkAdder(FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                                     VideoWatermarkService videoWatermarkService,
                                     FFprobeDevice fFprobeDevice, FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper,
                                     FFmpegDevice fFmpegDevice, FFmpegSubtitlesStreamConversionHelper subtitlesHelper,
                                     FontProperties fontProperties, CaptionGenerator captionGenerator) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.videoWatermarkService = videoWatermarkService;
        this.fFprobeDevice = fFprobeDevice;
        this.videoAudioConversionHelper = videoAudioConversionHelper;
        this.fFmpegDevice = fFmpegDevice;
        this.subtitlesHelper = subtitlesHelper;
        this.fontProperties = fontProperties;
        this.captionGenerator = captionGenerator;
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

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD,
                fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            fFmpegVideoHelper.validateVideoIntegrity(video);

            VideoWatermark watermark = videoWatermarkService.getWatermark(fileQueueItem.getUserId());
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().hideBanner().quite()
                    .input(video.getAbsolutePath());

            if (watermark.getWatermarkType() == VideoWatermarkType.IMAGE) {
                commandBuilder.useFilterComplex(true);
                SmartTempFile watermarkImage = fileQueueItem.getDownloadedFileOrThrow(watermark.getImage().getFileId());
                commandBuilder.input(watermarkImage.getAbsolutePath());
            }

            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(video.getAbsolutePath());
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                fFmpegVideoHelper.convertVideoCodecsForTelegramVideo(commandBuilder, allStreams, fileQueueItem.getFirstFileFormat(), fileQueueItem.getSize());
            } else {
                fFmpegVideoHelper.convertVideoCodecs(commandBuilder, allStreams, fileQueueItem.getFirstFileFormat(), result, fileQueueItem.getSize());
            }

            if (watermark.getWatermarkType() == VideoWatermarkType.TEXT) {
                commandBuilder.filterVideo(0, createTextWatermarkFilter(watermark));
            } else {
                if (commandBuilder.getComplexFilters().stream().anyMatch(f -> f.contains("[sv]"))) {
                    commandBuilder.complexFilter(createImageWatermarkFilter("sv", watermark));
                } else {
                    commandBuilder.complexFilter(createImageWatermarkFilter("0", watermark));
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

            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            String caption = captionGenerator.generate(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getSource());
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                FFprobeDevice.WHD wdh = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);

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
