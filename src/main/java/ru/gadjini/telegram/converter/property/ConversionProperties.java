package ru.gadjini.telegram.converter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("conversion")
public class ConversionProperties {

    private int calibreLongConversionTimeOut = 10 * 60;

    private int pdfToWordLongConversionTimeOut = 10 * 60;

    private String server;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getCalibreLongConversionTimeOut() {
        return calibreLongConversionTimeOut;
    }

    public void setCalibreLongConversionTimeOut(int calibreLongConversionTimeOut) {
        this.calibreLongConversionTimeOut = calibreLongConversionTimeOut;
    }

    public int getPdfToWordLongConversionTimeOut() {
        return pdfToWordLongConversionTimeOut;
    }

    public void setPdfToWordLongConversionTimeOut(int pdfToWordLongConversionTimeOut) {
        this.pdfToWordLongConversionTimeOut = pdfToWordLongConversionTimeOut;
    }
}
