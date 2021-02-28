package ru.gadjini.telegram.converter.service.image.device;

public interface Image2PdfDevice {

    default void convert2Pdf(String in, String out, String pdfTitle) throws InterruptedException {
        throw new UnsupportedOperationException();
    }
}
