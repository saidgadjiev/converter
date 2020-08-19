package ru.gadjini.telegram.converter.service.conversion.file;

import com.aspose.pdf.facades.PdfFileInfo;
import org.springframework.stereotype.Service;

@Service
public class FileValidator {

    public boolean isValidPdf(String path) {
        PdfFileInfo pdfFileInfo = new PdfFileInfo(path);

        return pdfFileInfo.isPdfFile();
    }
}
