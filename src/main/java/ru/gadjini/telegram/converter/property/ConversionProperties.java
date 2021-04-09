package ru.gadjini.telegram.converter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("conversion")
public class ConversionProperties {

    private int calibreLongConversionTimeOut = 10 * 60;

    private int asposeConversionTimeOut = 5 * 60;

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

    public int getAsposeConversionTimeOut() {
        return asposeConversionTimeOut;
    }

    public void setAsposeConversionTimeOut(int asposeConversionTimeOut) {
        this.asposeConversionTimeOut = asposeConversionTimeOut;
    }
}
