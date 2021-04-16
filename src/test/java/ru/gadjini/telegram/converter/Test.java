package ru.gadjini.telegram.converter;

import com.aspose.words.Document;
import com.aspose.words.License;
import com.aspose.words.SaveFormat;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.conversion.LocalProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessTimedOutException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Test {

    public static void main(String[] args) throws Throwable {
        new License().setLicense("C:\\Users\\GadzhievSA\\Work\\SCM\\converter\\license\\license-19.lic");
        LocalProcessExecutor localProcessExecutor = new LocalProcessExecutor();
        try {
            Document doc = new Document("C:\\1.txt");
            try {
                try {
                    System.out.println(LocalDateTime.now());
                    localProcessExecutor.execute(10*60, () -> {
                        try {
                            doc.save("C:\\1.pdf", SaveFormat.PDF);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new ConvertException(e);
                        }

                        return null;
                    }, () -> {
                        try {
                            doc.cleanup();
                        } catch (Exception e) {
                            throw new ConvertException(e);
                        }
                    });
                } catch (Throwable e) {
                    throw e;
                }
            } finally {
                doc.cleanup();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println(LocalDateTime.now());

        System.out.println("YES");
        Thread.sleep(2000);
        System.out.println("TT");
    }
}
