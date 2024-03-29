package ru.gadjini.telegram.converter.service.conversion.device;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.stream.Stream;

@Service
public class SmartCalibre {

    private static final String TAG = "calibre";

    private TempFileService tempFileService;

    private ProcessExecutor processExecutor;

    @Autowired
    public SmartCalibre(TempFileService tempFileService, ProcessExecutor processExecutor) {
        this.tempFileService = tempFileService;
        this.processExecutor = processExecutor;
    }

    public void convert(String in, String out, int waitForSeconds, String... options) throws InterruptedException {
        if (out.endsWith("doc")) {
            SmartTempFile tempFile = tempFileService.createTempFile(FileTarget.TEMP, TAG, Format.DOCX.getExt());
            try {
                processExecutor.execute(buildCommand(in, tempFile.getAbsolutePath(), options), waitForSeconds);

                Document document = new Document(tempFile.getAbsolutePath());
                try {
                    document.save(out, SaveFormat.DOC);
                } finally {
                    document.cleanup();
                }
            } catch (Exception e) {
                throw new ProcessException(e);
            } finally {
                tempFileService.delete(tempFile);
            }
        } else if (in.endsWith("doc")) {
            SmartTempFile tempFile = tempFileService.createTempFile(FileTarget.TEMP, TAG, Format.DOCX.getExt());
            try {
                Document document = new Document(in);
                try {
                    document.save(tempFile.getAbsolutePath(), SaveFormat.DOCX);
                } finally {
                    document.cleanup();
                }

                processExecutor.execute(buildCommand(tempFile.getAbsolutePath(), out, options), waitForSeconds);
            } catch (Exception e) {
                throw new ProcessException(e);
            } finally {
                tempFileService.delete(tempFile);
            }
        } else {
            processExecutor.execute(buildCommand(in, out, options), waitForSeconds);
        }
    }

    private String[] buildCommand(String in, String out, String... options) {
        return Stream.concat(
                Stream.of(
                        "ebook-convert",
                        in,
                        out
                ),
                Stream.of(options)
        ).toArray(String[]::new);
    }
}
