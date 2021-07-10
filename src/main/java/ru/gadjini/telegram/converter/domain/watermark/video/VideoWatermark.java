package ru.gadjini.telegram.converter.domain.watermark.video;

import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;

public class VideoWatermark {

    public static final String TYPE = "type";

    public static final String POSITION = "position";

    public static final String USER_ID = "user_id";

    public static final String TEXT = "wtext";

    public static final String IMAGE = "image";

    public static final String FONT_SIZE = "font_size";

    public static final String COLOR = "color";

    public static final String IMAGE_HEIGHT = "image_height";

    public static final String TRANSPARENCY = "transparency";

    private VideoWatermarkType watermarkType;

    private VideoWatermarkPosition watermarkPosition;

    private long userId;

    private String text;

    private TgFile image;

    private Integer imageHeight;

    private String transparency;

    private Integer fontSize;

    private VideoWatermarkColor color;

    public VideoWatermarkType getWatermarkType() {
        return watermarkType;
    }

    public void setWatermarkType(VideoWatermarkType watermarkType) {
        this.watermarkType = watermarkType;
    }

    public VideoWatermarkPosition getWatermarkPosition() {
        return watermarkPosition;
    }

    public void setWatermarkPosition(VideoWatermarkPosition watermarkPosition) {
        this.watermarkPosition = watermarkPosition;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public TgFile getImage() {
        return image;
    }

    public void setImage(TgFile image) {
        this.image = image;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public VideoWatermarkColor getColor() {
        return color;
    }

    public void setColor(VideoWatermarkColor color) {
        this.color = color;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public String getTransparency() {
        return transparency;
    }

    public void setTransparency(String transparency) {
        this.transparency = transparency;
    }
}
