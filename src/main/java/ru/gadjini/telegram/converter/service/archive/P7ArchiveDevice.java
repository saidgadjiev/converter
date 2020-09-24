package ru.gadjini.telegram.converter.service.archive;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.condition.LinuxMacCondition;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Conditional(LinuxMacCondition.class)
public class P7ArchiveDevice extends BaseArchiveDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public P7ArchiveDevice(ProcessExecutor processExecutor) {
        super(Set.of(Format.ZIP));
        this.processExecutor = processExecutor;
    }

    @Override
    public void zip(List<String> files, String out) {
        processExecutor.execute(buildCommand(files, out));
    }

    @Override
    public String rename(String archive, String fileHeader, String newFileName) {
        String newHeader = buildNewHeader(fileHeader, newFileName);
        processExecutor.execute(buildRenameCommand(archive, fileHeader, newHeader));

        return newHeader;
    }

    private String[] buildRenameCommand(String archive, String fileHeader, String newFileHeader) {
        return new String[]{
                "7z", "rn", archive, fileHeader, newFileHeader.replace("@", "")
        };
    }

    private String buildNewHeader(String fileHeader, String newFileName) {
        String path = FilenameUtils.getFullPath(fileHeader);

        return path + newFileName;
    }

    private String[] buildCommand(List<String> files, String out) {
        List<String> command = new ArrayList<>();
        command.add("7z");
        command.add("a");
        command.add(out);
        command.addAll(files);

        return command.toArray(new String[0]);
    }

    public static void main(String[] args) {
        System.out.println(FilenameUtils.getFullPath("1.pdf"));
    }
}
