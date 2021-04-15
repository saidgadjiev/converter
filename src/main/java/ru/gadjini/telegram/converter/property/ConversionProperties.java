package ru.gadjini.telegram.converter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("conversion")
public class ConversionProperties {

    private int conversionTimeOut = 10 * 60;

    private String server;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getConversionTimeOut() {
        return conversionTimeOut;
    }

    public void setConversionTimeOut(int conversionTimeOut) {
        this.conversionTimeOut = conversionTimeOut;
    }
}
