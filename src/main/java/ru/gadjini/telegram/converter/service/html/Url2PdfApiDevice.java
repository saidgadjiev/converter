package ru.gadjini.telegram.converter.service.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.property.ConversionProperties;
import ru.gadjini.telegram.converter.service.ProcessExecutor;
import ru.gadjini.telegram.converter.utils.UrlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Qualifier("api")
public class Url2PdfApiDevice implements HtmlDevice {

    public static final String PDF_OUTPUT = "pdf";

    public static final String SCREENSHOT_OUTPUT = "screenshot";

    private ConversionProperties conversionProperties;

    @Autowired
    public Url2PdfApiDevice(ConversionProperties conversionProperties) {
        this.conversionProperties = conversionProperties;
    }

    @Override
    public void convertHtml(String html, String out, String outputType) {
        new ProcessExecutor().execute(buildCommandByHtml(html, out, outputType));
    }

    @Override
    public void convertUrl(String url, String out, String outputType) {
        new ProcessExecutor().execute(buildCommandByUrl(prepareUrl(UrlUtils.appendScheme(url)), out, outputType));
    }

    private String prepareUrl(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    private String[] buildCommandByUrl(String url, String out, String output) {
        return new String[]{"curl", "-XGET", getConversionUrlByUrl(url, output), "-o", out};
    }

    private String[] buildCommandByHtml(String html, String out, String output) {
        return new String[]{"curl", "-XPOST", "-d@" + html, "-H", "content-type: text/html", getConversionUrlByHtml(output), "-o", out};
    }

    private String getConversionUrlByUrl(String url, String output) {
        if (output.equals(PDF_OUTPUT)) {
            return getBaseApi() + "?url=" + url + "&ignoreHttpsErrors=true&goto.timeout=60000&goto.waitUntil=load&pdf.height=auto&output=" + output;
        } else {
            return getBaseApi() + "?url=" + url + "&ignoreHttpsErrors=true&goto.timeout=60000&goto.waitUntil=load&output=" + output;
        }
    }

    private String getConversionUrlByHtml(String output) {
        return getBaseApi() + "?&ignoreHttpsErrors=true&pdf.height=auto&output=" + output;
    }

    private String getBaseApi() {
        return conversionProperties.getServer() + "/api/render";
    }
}
