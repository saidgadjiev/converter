package ru.gadjini.telegram.converter.service.conversion.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Qualifier("djvu")
public class DjvuLibre implements ConvertDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public DjvuLibre(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    @Override
    public void convert(String in, String out, String... options) {
        processExecutor.execute(getCommand(in, out, options));
    }

    private String[] getCommand(String in, String out, String... options) {
        List<String> command = new ArrayList<>();
        command.add("ddjvu");
        command.addAll(Arrays.asList(options));
        command.add(in);
        command.add(out);

        return command.toArray(String[]::new);
    }
}
