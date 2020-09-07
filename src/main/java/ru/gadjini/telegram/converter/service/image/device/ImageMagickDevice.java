package ru.gadjini.telegram.converter.service.image.device;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageMagickDevice implements ImageConvertDevice {

    @Override
    public void convert(String in, String out, String... options) {
        new ProcessExecutor().execute(getCommand(in, out, options));
    }

    private String[] getCommand(String in, String out, String... options) {
        List<String> command = new ArrayList<>(convertCommandName());
        command.add("-background");
        command.add("none");
        command.addAll(Arrays.asList(options));
        command.add(in);
        command.add(out);

        return command.toArray(new String[0]);
    }

    private List<String> convertCommandName() {
        return System.getProperty("os.name").contains("Windows") ? List.of("magick", "convert") : List.of("convert");
    }
}
