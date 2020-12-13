package ru.gadjini.telegram.converter.service.conversion.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class PdfUniteDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public PdfUniteDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public void mergePdfs(Collection<String> pdfs, String out) {
        processExecutor.execute(buildMergeCommand(pdfs, out));
    }

    private String[] buildMergeCommand(Collection<String> pdfs, String out) {
        List<String> command = new ArrayList<>();
        command.add("pdfunite");
        command.addAll(pdfs);
        command.add(out);

        return command.toArray(new String[0]);
    }
}
