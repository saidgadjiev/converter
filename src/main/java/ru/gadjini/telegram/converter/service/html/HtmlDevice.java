package ru.gadjini.telegram.converter.service.html;

public interface HtmlDevice {

    void convertHtml(String html, String out, String outputType);

    void convertUrl(String url, String out, String outputType);

}
