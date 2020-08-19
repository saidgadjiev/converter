package ru.gadjini.telegram.converter.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.common.CommandNames;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.converter.model.bot.api.object.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InlineKeyboardService {

    private ButtonFactory buttonFactory;

    @Autowired
    public InlineKeyboardService(ButtonFactory buttonFactory) {
        this.buttonFactory = buttonFactory;
    }

    public InlineKeyboardMarkup getArchiveCreatingKeyboard(int jobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelArchiveCreatingQuery(jobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getThumbProcessingKeyboard(int jobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelRenameQuery(jobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getRenameProcessingKeyboard(int jobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelRenameQuery(jobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getFilesListKeyboard(Set<Integer> filesIds, int limit, int prevLimit, int offset, int unzipJobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        if (!(offset == 0 && filesIds.size() == limit)) {
            if (filesIds.size() == offset + limit) {
                inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.toPrevPage(CommandNames.UNZIP_COMMAND_NAME, limit, Math.max(0, offset - prevLimit), locale)));
            } else if (offset == 0) {
                inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.toNextPage(CommandNames.UNZIP_COMMAND_NAME, limit, offset + limit, locale)));
            } else {
                inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.toPrevPage(CommandNames.UNZIP_COMMAND_NAME, limit, Math.max(0, offset - prevLimit), locale),
                        buttonFactory.toNextPage(CommandNames.UNZIP_COMMAND_NAME, limit, offset + limit, locale)));
            }
        }
        List<List<Integer>> lists = Lists.partition(filesIds.stream().skip(offset).limit(limit).collect(Collectors.toCollection(ArrayList::new)), 4);
        int i = offset + 1;
        for (List<Integer> list : lists) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int id : list) {
                row.add(buttonFactory.extractFileButton(String.valueOf(i++), id, unzipJobId));
            }

            inlineKeyboardMarkup.getKeyboard().add(row);
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.extractAllButton(unzipJobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getFilesListKeyboard(Set<Integer> filesIds, int unzipJobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        int i = 1;
        List<List<Integer>> lists = Lists.partition(new ArrayList<>(filesIds), 4);
        for (List<Integer> list : lists) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int id : list) {
                row.add(buttonFactory.extractFileButton(String.valueOf(i++), id, unzipJobId));
            }

            inlineKeyboardMarkup.getKeyboard().add(row);
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.extractAllButton(unzipJobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getArchiveFilesKeyboard(Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelArchiveFiles(locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getUnzipProcessingKeyboard(int jobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelUnzipQuery(jobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getExtractFileProcessingKeyboard(int jobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelExtractFileQuery(jobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup reportKeyboard(int queueItemId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.report(queueItemId, locale)));
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup inlineKeyboardMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        inlineKeyboardMarkup.setKeyboard(new ArrayList<>());

        return inlineKeyboardMarkup;
    }
}
