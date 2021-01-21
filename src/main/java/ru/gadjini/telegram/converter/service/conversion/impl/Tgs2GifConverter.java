package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

@Component
public class Tgs2GifConverter extends BaseAny2AnyConverter {

    public static final String TAG = "tgs2";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.TGS), List.of(Format.GIF)
    );

    private ProcessExecutor processExecutor;

    @Autowired
    public Tgs2GifConverter(ProcessExecutor processExecutor) {
        super(MAP);
        this.processExecutor = processExecutor;
    }

    @Override
    public ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        return toGiff(fileQueueItem);
    }

    private FileResult toGiff(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile result = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, Format.GIF.getExt());
        try {
            processExecutor.execute(command(file.getAbsolutePath(), result.getAbsolutePath()));

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.GIF.getExt());
            return new FileResult(fileName, result);
        } catch (Exception ex) {
            result.smartDelete();
            throw ex;
        }
    }

    @Override
    protected void doDeleteFiles(ConversionQueueItem fileQueueItem) {
        fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId()).smartDelete();
    }

    private String[] command(String in, String out) {
        return new String[]{"node", "tgs-to-gif/cli.js", in, "--out", out};
    }
}
