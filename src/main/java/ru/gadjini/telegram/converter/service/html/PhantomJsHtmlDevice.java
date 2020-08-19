package ru.gadjini.telegram.converter.service.html;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.service.ProcessExecutor;
import ru.gadjini.telegram.converter.utils.UrlUtils;

@Service
@Qualifier("phantomjs")
public class PhantomJsHtmlDevice implements HtmlDevice {

    @Override
    public void processHtml(String urlOrHtml, String out) {
        new ProcessExecutor().execute(buildCommand(urlOrHtml, out));
    }

    @Override
    public void processUrl(String url, String out) {
        new ProcessExecutor().execute(buildCommand(UrlUtils.appendScheme(url), out));
    }

    private String[] buildCommand(String urlOrHtml, String out) {
        return new String[]{
                isWindows() ? "phantomjs.cmd" : "phantomjs",
                "rasterize.js",
                "\"" + urlOrHtml + "\"",
                out
        };
    }

    private boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }
}
