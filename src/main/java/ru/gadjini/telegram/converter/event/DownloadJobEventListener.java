package ru.gadjini.telegram.converter.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.file.FileDownloadService;
import ru.gadjini.telegram.smart.bot.commons.service.queue.event.DownloadCompleted;

import java.util.ListIterator;

@Component
public class DownloadJobEventListener {

    private Jackson json;

    private FileDownloadService fileDownloadService;

    @Autowired
    public DownloadJobEventListener(Jackson jackson, FileDownloadService fileDownloadService) {
        this.json = jackson;
        this.fileDownloadService = fileDownloadService;
    }

    @EventListener
    public void downloadCompleted(DownloadCompleted downloadCompleted) {
        if (downloadCompleted.getDownloadQueueItem().getExtra() != null) {
            DownloadExtra downloadExtra = json.convertValue(downloadCompleted.getDownloadQueueItem().getExtra(), DownloadExtra.class);
            ListIterator<TgFile> listIterator = downloadExtra.getFiles().listIterator(downloadExtra.getCurrentFileIndex() + 1);

            if (listIterator.hasNext()) {
                TgFile file = listIterator.next();
                fileDownloadService.createDownload(file, downloadCompleted.getDownloadQueueItem().getProducerId(),
                        downloadCompleted.getDownloadQueueItem().getUserId(), new DownloadExtra(downloadExtra.getFiles(), downloadExtra.getCurrentFileIndex() + 1));
            }
        }
    }
}
