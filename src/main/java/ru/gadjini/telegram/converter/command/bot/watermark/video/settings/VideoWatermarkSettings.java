package ru.gadjini.telegram.converter.command.bot.watermark.video.settings;

import ru.gadjini.telegram.converter.command.bot.watermark.video.state.WatermarkStateName;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkColor;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkPosition;
import ru.gadjini.telegram.converter.domain.watermark.video.VideoWatermarkType;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;

public class VideoWatermarkSettings {

    private VideoWatermarkType watermarkType;

    private VideoWatermarkPosition watermarkPosition;

    private String text;

    private MessageMedia image;

    private String fontSize;

    private VideoWatermarkColor color;

    private Integer imageWidth;

    private WatermarkStateName stateName;

    public VideoWatermarkType getWatermarkType() {
        return watermarkType;
    }

    public void setWatermarkType(VideoWatermarkType watermarkType) {
        this.watermarkType = watermarkType;
    }

    public WatermarkStateName getStateName() {
        return stateName;
    }

    public void setStateName(WatermarkStateName stateName) {
        this.stateName = stateName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public VideoWatermarkPosition getWatermarkPosition() {
        return watermarkPosition;
    }

    public void setWatermarkPosition(VideoWatermarkPosition watermarkPosition) {
        this.watermarkPosition = watermarkPosition;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public VideoWatermarkColor getColor() {
        return color;
    }

    public void setColor(VideoWatermarkColor color) {
        this.color = color;
    }

    public MessageMedia getImage() {
        return image;
    }

    public void setImage(MessageMedia image) {
        this.image = image;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }
}
