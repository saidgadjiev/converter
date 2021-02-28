package ru.gadjini.telegram.converter.service.image.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageMagickDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public ImageMagickDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public void convert2Image(String in, String out, String... options) throws InterruptedException {
        processExecutor.execute(get2ImageConvertCommand(in, out, options));
    }

    public void convert2Tiff(String in, String out) throws InterruptedException {
        processExecutor.execute(getImagesConvertCommand(List.of(in), out));
    }

    public void changeFormatAndRemoveAlphaChannel(String in, String format) throws InterruptedException {
        processExecutor.execute(getChangeFormatAndRemoveAlphaCommand(in, format));
    }

    private String[] getChangeFormatAndRemoveAlphaCommand(String in, String format) {
        return new String[]{
                "mogrify",
                "-format",
                format,
                "-background",
                "white",
                "-alpha",
                "remove",
                "-alpha",
                "off",
                in
        };
    }

    private String[] getImagesConvertCommand(List<String> in, String out) {
        List<String> command = new ArrayList<>(convertCommandName());
        command.add(String.join(" ", in));
        command.add("-resize");
        command.add("595x843>");
        command.add(out);

        return command.toArray(new String[0]);
    }

    private String[] get2ImageConvertCommand(String in, String out, String... options) {
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
