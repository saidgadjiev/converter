package ru.gadjini.telegram.converter.service.image.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.aspose.AsposePdfService;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageMagickDevice implements ImageConvertDevice {

    private AsposePdfService asposePdfService;

    @Autowired
    public ImageMagickDevice(AsposePdfService asposePdfService) {
        this.asposePdfService = asposePdfService;
    }

    @Override
    public void convert2Image(String in, String out, String... options) {
        new ProcessExecutor().execute(get2ImageConvertCommand(in, out, options));
    }

    @Override
    public void convert2Format(String in, String format) {
        new ProcessExecutor().execute(getMogrifyCommand(in, format));
    }

    @Override
    public void convert2Pdf(String in, String out, String pdfTitle) {
        new ProcessExecutor().execute(get2PdfConvertCommand(List.of(in), out));
        asposePdfService.setPdfTitle(out, pdfTitle);
    }

    private String[] get2PdfConvertCommand(List<String> in, String out) {
        List<String> command = new ArrayList<>(convertCommandName());
        command.add(String.join(" ", in));
        command.add("-resize");
        command.add("595x843>");
        command.add("-gravity");
        command.add("center");
        command.add("-page");
        command.add("a4");
        command.add(out);

        return command.toArray(new String[0]);
    }

    private String[] getMogrifyCommand(String in, String format) {
        return new String[] {
                "mogrify",
                "-format",
                format,
                in
        };
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
