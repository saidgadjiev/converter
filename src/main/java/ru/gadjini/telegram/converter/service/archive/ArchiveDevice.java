package ru.gadjini.telegram.converter.service.archive;

import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

public interface ArchiveDevice {

    void zip(List<String> files, String out);

    default String rename(String archive, String fileHeader, String newFileName) {
        return null;
    }

    boolean accept(Format format);
}
