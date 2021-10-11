package ru.gadjini.telegram.converter.event;

import com.aspose.words.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.job.ConversionWorkerFactory;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.queue.event.QueueJobInitialization;

import java.util.Set;

@Component
public class QueueJobEventListener implements ApplicationListener<QueueJobInitialization> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionWorkerFactory.class);

    private Set<FormatCategory> categories;

    @Autowired
    public void setCategories(Set<FormatCategory> categories) {
        this.categories = categories;
    }

    @Override
    public void onApplicationEvent(QueueJobInitialization event) {
        if (categories.contains(FormatCategory.DOCUMENTS)) {
            applyAsposeLicenses();
        }
    }

    private void applyAsposeLicenses() {
        try {
            new License().setLicense("license/license-19.lic");
            LOGGER.debug("Word license applied");

            new com.aspose.pdf.License().setLicense("license/license-19.lic");
            LOGGER.debug("Pdf license applied");

            new com.aspose.imaging.License().setLicense("license/license-19.lic");
            LOGGER.debug("Imaging license applied");

            new com.aspose.slides.License().setLicense("license/license-19.lic");
            LOGGER.debug("Slides license applied");

            new com.aspose.cells.License().setLicense("license/license-19.lic");
            LOGGER.debug("Cells license applied");
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}
