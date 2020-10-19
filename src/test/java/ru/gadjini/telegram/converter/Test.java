package ru.gadjini.telegram.converter;

import com.aspose.slides.License;
import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;

public class Test {

    public static void main(String[] args) {
        new License().setLicense("C:\\Users\\GadzhievSA\\Work\\SCM\\any2any\\license\\license-19.lic");
        Presentation presentation = new Presentation("C:\\Презентация 99.pptx");

        try {
            presentation.save("C:\\tt.pdf", SaveFormat.Pdf);
        } finally {
            presentation.dispose();
        }
    }
}
