package ru.gadjini.telegram.converter.service.language;

import com.detectlanguage.DetectLanguage;
import com.detectlanguage.errors.APIError;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.property.DetectLanguageProperties;

@Service
public class DetectLanguageService implements LanguageDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetectLanguageService.class);

    @Autowired
    public DetectLanguageService(DetectLanguageProperties detectLanguageProperties) {
        DetectLanguage.apiKey = detectLanguageProperties.getKey();
    }

    @Override
    public String detect(String text) {
        try {
            String language = DetectLanguage.simpleDetect(text);

            if (StringUtils.isBlank(language)) {
                LOGGER.debug("Language not detected({})", StringUtils.substring(text, 0, 50));
            }

            return language;
        } catch (APIError apiError) {
            LOGGER.error(apiError.getMessage() + " code " + apiError.code, apiError);
            return null;
        }
    }
}
