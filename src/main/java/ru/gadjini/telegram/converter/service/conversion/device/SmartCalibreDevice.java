package ru.gadjini.telegram.converter.service.conversion.device;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.stream.Stream;

@Component
@Qualifier("calibre")
public class SmartCalibreDevice implements ConvertDevice {

    private static final String TAG = "calibre";

    private TempFileService tempFileService;

    private ProcessExecutor processExecutor;

    @Autowired
    public SmartCalibreDevice(TempFileService tempFileService, ProcessExecutor processExecutor) {
        this.tempFileService = tempFileService;
        this.processExecutor = processExecutor;
    }

    @Override
    public void convert(String in, String out, String title, String... options) {
        if (out.endsWith("doc")) {
            SmartTempFile tempFile = tempFileService.createTempFile(TAG, Format.DOCX.getExt());
            try {
                processExecutor.execute(buildCommand(in, tempFile.getAbsolutePath(), title, options));

                Document document = new Document(tempFile.getAbsolutePath());
                try {
                    document.save(out, SaveFormat.DOC);
                } finally {
                    document.cleanup();
                }
            } catch (Exception e) {
                throw new ProcessException(e);
            } finally {
                tempFile.smartDelete();
            }
        } else if (in.endsWith("doc")) {
            SmartTempFile tempFile = tempFileService.createTempFile(TAG, Format.DOCX.getExt());
            try {
                Document document = new Document(in);
                try {
                    document.save(tempFile.getAbsolutePath(), SaveFormat.DOCX);
                } finally {
                    document.cleanup();
                }

                processExecutor.execute(buildCommand(tempFile.getAbsolutePath(), out, title, options));
            } catch (Exception e) {
                throw new ProcessException(e);
            } finally {
                tempFile.smartDelete();
            }
        } else {
            processExecutor.execute(buildCommand(in, out, title, options));
        }
    }

    private String[] buildCommand(String in, String out, String title, String... options) {
        return Stream.concat(
                Stream.of(
                        "ebook-convert",
                        in,
                        out,
                        "--title",
                        title
                ),
                Stream.of(options)
        ).toArray(String[]::new);
    }
}
