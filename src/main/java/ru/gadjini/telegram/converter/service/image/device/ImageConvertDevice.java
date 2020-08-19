package ru.gadjini.telegram.converter.service.image.device;

public interface ImageConvertDevice {

    void convert(String in, String out, String ... options);

    void negativeTransparent(String in, String out, String inaccuracy, String... colors);

    void positiveTransparent(String in, String out, String inaccuracy, String color);

    void applyBlackAndWhiteFilter(String in, String out);

    void applyNegativeFilter(String in, String out);

    void applySketchFilter(String in, String out);

    void resize(String in, String out, String size);

    void convertToThumb(String in, String out);
}
