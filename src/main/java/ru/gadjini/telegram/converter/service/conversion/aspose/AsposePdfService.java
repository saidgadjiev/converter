package ru.gadjini.telegram.converter.service.conversion.aspose;

import com.aspose.pdf.Document;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class AsposePdfService {

    public void setPdfTitle(String pdf, String title) {
        if (StringUtils.isBlank(title)) {
            return;
        }
        Document document = new Document(pdf);

        try {
            document.setTitle(title);
            document.save();
        } finally {
            document.dispose();
        }
    }
}
