package ru.gadjini.telegram.converter.service.image.device;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.condition.LinuxMacCondition;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

@Component("img2pdf")
@Conditional(LinuxMacCondition.class)
public class Img2PdfDevice implements Image2PdfDevice {

    @Override
    public void convert2Pdf(String in, String out, String pdfTitle) {
        new ProcessExecutor().execute(getCommand(in, out, pdfTitle));
    }

    private String[] getCommand(String in, String out, String title) {
        return new String[]{
                "bash", "-c", "img2pdf -o " + out + " -S A4 --imgsize 210mmx297mm --fit shrink --title " + title + " " + in
        };
    }
}
