package ru.gadjini.telegram.converter.service.conversion.impl.merge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

@Component
public class FilesListCreator {

    private TempFileService tempFileService;

    @Autowired
    public FilesListCreator(TempFileService tempFileService) {
        this.tempFileService = tempFileService;
    }

    public SmartTempFile createFilesList(long userId, List<SmartTempFile> files) {
        SmartTempFile filesList = tempFileService.createTempFile(FileTarget.TEMP, userId, "flist", Format.TXT.getExt());
        try (PrintWriter printWriter = new PrintWriter(filesList.getAbsolutePath())) {
            for (SmartTempFile downloadedFile : files) {
                printWriter.println(filesListFileStr(downloadedFile.getAbsolutePath()));
            }

            return filesList;
        } catch (FileNotFoundException e) {
            tempFileService.delete(filesList);
            throw new ConvertException(e);
        }
    }

    private String filesListFileStr(String filePath) {
        return "file '" + filePath + "'";
    }

}
