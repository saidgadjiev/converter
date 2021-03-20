package ru.gadjini.telegram.converter.service.image.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.device.ConvertDevice;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component("jpeg2epub")
public class JpegEpubDevice implements ConvertDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public JpegEpubDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    @Override
    public void convert(String in, String out, String... options) throws InterruptedException {
        processExecutor.execute(getCommand(in, out, options));
    }

    private String[] getCommand(String in, String out, String... options) {
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add("jpegtoepub.py");
        command.addAll(Arrays.asList(options));
        command.add(in);
        command.add("-o");
        command.add(out);

        return command.toArray(String[]::new);
    }
}
