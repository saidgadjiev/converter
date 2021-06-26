package ru.gadjini.telegram.converter.command.bot.merge;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.Locale;

public interface MergeFilesConfigurator {

    String getMediaTypeName(Locale locale);

    boolean isValidFormat(Format format);

    String getCommandName();

    boolean accept(Message message);

    Format getTargetFormat();

    int getMaxFiles();
}
