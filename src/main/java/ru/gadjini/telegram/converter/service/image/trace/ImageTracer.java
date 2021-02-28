package ru.gadjini.telegram.converter.service.image.trace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

@Component
public class ImageTracer {

    private ProcessExecutor processExecutor;

    @Autowired
    public ImageTracer(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public void trace(String in, String out) throws InterruptedException {
        processExecutor.execute(command(in, out));
    }

    private String[] command(String in, String out) {
        return new String[]{"java", "-jar", "ImageTracer.jar", in, "outfilename", out};
    }
}
