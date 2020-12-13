package ru.gadjini.telegram.converter.service.conversion.device;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.List;

@Component
public class P7ArchiveDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    protected P7ArchiveDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public void zip(String filesParentDir, String out) {
        processExecutor.execute(buildZipCommand(filesParentDir, out));
    }

    private String[] buildZipCommand(String filesParentDir, String out) {
        List<String> command = new ArrayList<>();
        command.add("7z");
        command.add("a");
        command.add(out);
        command.add(filesParentDir);

        return command.toArray(new String[0]);
    }

    public static void main(String[] args) {
        System.out.println(FilenameUtils.getFullPath("1.pdf"));
    }
}
