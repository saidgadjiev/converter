package ru.gadjini.telegram.converter.property;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("conversion")
public class ConversionProperties {

    @Value("${keep.downloads:false}")
    private boolean keepDownloads;

    public boolean isKeepDownloads() {
        return keepDownloads;
    }

    public void setKeepDownloads(boolean keepDownloads) {
        this.keepDownloads = keepDownloads;
    }
}
