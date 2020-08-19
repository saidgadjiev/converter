package ru.gadjini.telegram.converter.service.image.trace;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.ProcessExecutor;

@Component
public class ImageTracer {

    public void trace(String in, String out) {
        new ProcessExecutor().execute(command(in, out));
    }

    private String[] command(String in, String out) {
        return new String[]{"java", "-jar", "ImageTracer.jar", in, "outfilename", out};
    }
}
