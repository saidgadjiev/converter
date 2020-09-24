package ru.gadjini.telegram.converter.service.image.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.condition.WindowsCondition;
import ru.gadjini.telegram.converter.service.conversion.aspose.AsposePdfService;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.List;

@Component
@Conditional(WindowsCondition.class)
public class Magick2PdfDevice implements Image2PdfDevice {

    private AsposePdfService asposePdfService;

    private ProcessExecutor processExecutor;

    @Autowired
    public Magick2PdfDevice(AsposePdfService asposePdfService, ProcessExecutor processExecutor) {
        this.asposePdfService = asposePdfService;
        this.processExecutor = processExecutor;
    }

    @Override
    public void convert2Pdf(String in, String out, String pdfTitle) {
        processExecutor.execute(get2PdfConvertCommand(List.of(in), out));
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

    private List<String> convertCommandName() {
        return System.getProperty("os.name").contains("Windows") ? List.of("magick", "convert") : List.of("convert");
    }
}
