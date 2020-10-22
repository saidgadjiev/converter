package ru.gadjini.telegram.converter.service.image.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.condition.LinuxMacCondition;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

@Component("img2pdf")
@Conditional(LinuxMacCondition.class)
public class Img2PdfDevice implements Image2PdfDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public Img2PdfDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    @Override
    public void convert2Pdf(String in, String out, String pdfTitle) {
        processExecutor.execute(getCommand(in, out, pdfTitle));
    }

    private String[] getCommand(String in, String out, String title) {
        return new String[]{
                "bash", "-c", "img2pdf -o " + out + " --pillow-limit-break -S A4 --imgsize 210mmx297mm --fit shrink --title \"" + title + "\" " + in
        };
    }
}
