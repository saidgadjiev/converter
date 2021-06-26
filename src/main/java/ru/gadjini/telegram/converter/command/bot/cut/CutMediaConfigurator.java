package ru.gadjini.telegram.converter.command.bot.cut;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.Locale;

public interface CutMediaConfigurator {

    boolean accept(Message message);

    String getMediaTypeName(Locale locale);

    FormatCategory getFormatCategory();

    String getCommandName();
}
