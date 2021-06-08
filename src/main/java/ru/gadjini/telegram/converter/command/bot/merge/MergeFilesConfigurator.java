package ru.gadjini.telegram.converter.command.bot.merge;

import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public interface MergeFilesConfigurator {

    String getFileType();

    boolean isValidFormat(Format format);

    String getCommandName();
}
