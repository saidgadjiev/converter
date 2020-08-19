package ru.gadjini.telegram.converter.service.text;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.service.language.LanguageDetector;

import java.util.Set;

@Service
public class TextDetector {

    private static final Set<String> RL_LANGUAGES = Set.of("ar", "az", "he", "fa", "ur");

    private LanguageDetector languageDetector;

    @Autowired
    public TextDetector(LanguageDetector languageDetector) {
        this.languageDetector = languageDetector;
    }

    public TextInfo detect(String text) {
        TextInfo textInfo = new TextInfo();

        String languageCode = languageDetector.detect(text);
        textInfo.setLanguageCode(languageCode);
        textInfo.setDirection(getDirection(textInfo.getLanguageCode()));
        textInfo.setFont(getFont(textInfo.getLanguageCode()));

        return textInfo;
    }

    private Font getFont(String language) {
        if (StringUtils.isBlank(language)) {
            return Font.TIMES_NEW_ROMAN;
        }
        for (Font font : Font.values()) {
            if (font.isSupportedLanguage(language)) {
                return font;
            }
        }

        return Font.TIMES_NEW_ROMAN;
    }

    private TextDirection getDirection(String language) {
        if (StringUtils.isBlank(language)) {
            return TextDirection.LR;
        }
        if (RL_LANGUAGES.contains(language)) {
            return TextDirection.RL;
        }

        return TextDirection.LR;
    }
}
