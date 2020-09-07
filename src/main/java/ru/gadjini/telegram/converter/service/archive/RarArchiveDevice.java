package ru.gadjini.telegram.converter.service.archive;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.condition.LinuxMacCondition;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.conversion.api.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Conditional(LinuxMacCondition.class)
public class RarArchiveDevice extends BaseArchiveDevice {

    @Autowired
    public RarArchiveDevice() {
        super(Set.of(Format.RAR));
    }

    @Override
    public void zip(List<String> files, String out) {
        new ProcessExecutor().execute(buildCommand(files, out));
    }

    @Override
    public String rename(String archive, String fileHeader, String newFileName) {
        String newHeader = buildNewHeader(fileHeader, newFileName);
        new ProcessExecutor().execute(buildRenameCommand(archive, fileHeader, newHeader));

        return newHeader;
    }

    private String[] buildRenameCommand(String archive, String fileHeader, String newFileHeader) {
        return new String[] {"rar", "rn", archive, fileHeader, newFileHeader};
    }

    private String buildNewHeader(String fileHeader, String newFileName) {
        String path = FilenameUtils.getFullPath(fileHeader);

        return path + newFileName;
    }

    private String[] buildCommand(List<String> files, String out) {
        List<String> command = new ArrayList<>();
        command.add("rar");
        command.add("a");
        command.add("-ep");
        command.add(out);
        command.addAll(files);

        return command.toArray(new String[0]);
    }
}
