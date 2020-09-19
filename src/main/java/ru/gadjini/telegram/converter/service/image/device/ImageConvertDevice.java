package ru.gadjini.telegram.converter.service.image.device;

public interface ImageConvertDevice {

    void convert2Image(String in, String out, String... options);

    void convert2Format(String in, String format);

    void convert2Pdf(String in, String out, String pdfTitle);

}
