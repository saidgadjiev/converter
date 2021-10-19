package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermark;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkPosition;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkType;
import ru.gadjini.telegram.converter.property.FontProperties;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

import java.io.File;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.TGS;

public class FFmpegVideoWatermarkCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FontProperties fontProperties;

    public FFmpegVideoWatermarkCommandBuilder(FontProperties fontProperties) {
        this.fontProperties = fontProperties;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        VideoWatermark watermark = conversionContext.getExtra(FFmpegConversionContext.VIDEO_WATERMARK);

        if (watermark.getWatermarkType() == VideoWatermarkType.TEXT) {
            command.filterVideo(0, createTextWatermarkFilter(watermark));
        } else {
            if (command.getComplexFilters().stream().anyMatch(f -> f.contains("[sv]"))) {
                command.complexFilter(createWatermarkFileFilter("sv", watermark));
            } else {
                command.complexFilter(createWatermarkFileFilter("0", watermark));
            }
        }
        command.complexFilters();

        super.prepareCommand(command, conversionContext);
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

    private String createTextWatermarkFilter(VideoWatermark videoWatermark) {
        StringBuilder filter = new StringBuilder();

        filter.append("drawtext=text='").append(videoWatermark.getText()).append("':")
                .append(getTextXY(videoWatermark.getWatermarkPosition())).append(":")
                .append("fontfile=").append(getFontPath("tahoma")).append(":")
                .append("fontsize=").append(videoWatermark.getFontSize() == null ? "(h/30)" : videoWatermark.getFontSize()).append(":")
                .append("fontcolor=").append(videoWatermark.getColor().name().toLowerCase());

        return filter.toString();
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
