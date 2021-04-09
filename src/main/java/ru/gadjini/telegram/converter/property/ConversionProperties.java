package ru.gadjini.telegram.converter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("conversion")
public class ConversionProperties {

    private int calibreConversionTimeOut = 10 * 60;

    private int asposeConversionTimeOut = 5 * 60;

    private String server;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getCalibreConversionTimeOut() {
        return calibreConversionTimeOut;
    }

    public void setCalibreConversionTimeOut(int calibreConversionTimeOut) {
        this.calibreConversionTimeOut = calibreConversionTimeOut;
    }

    public int getAsposeConversionTimeOut() {
        return asposeConversionTimeOut;
    }

    public void setAsposeConversionTimeOut(int asposeConversionTimeOut) {
        this.asposeConversionTimeOut = asposeConversionTimeOut;
    }
}
