package ru.gadjini.telegram.converter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("font")
public class FontProperties {

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
