package ru.gadjini.telegram.converter.service.conversion.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class PdfToPpmDevice implements ConvertDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public PdfToPpmDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    @Override
    public void convert(String in, String out, String... options) throws InterruptedException {
        processExecutor.execute(buildCommand(in, out, options));
    }

    private String[] buildCommand(String in, String out, String... options) {
        List<String> command = new ArrayList<>();
        command.add("pdftoppm");
        command.add(in);
        command.add(out);
        command.addAll(Arrays.asList(options));

        return command.toArray(String[]::new);
    }
}
