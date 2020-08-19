package ru.gadjini.telegram.converter.service.text;

import java.util.Set;

public enum Font {

    MANGAL("Mangal", Set.of("mr", "hi", "pa", "bn"), 13),
    LATHA("Latha", Set.of("ta"), 13),
    JAMEEL_NOORI_NASTALEEQ_KASHEEDA("Jameel Noori Nastaleeq Kasheeda", Set.of("ur"), 14),
    TIMES_NEW_ROMAN("TimesNewRoman", null, 13);

    private String fontName;

    private Set<String> languages;

    private int primarySize;

    Font(String fontName, Set<String> languages, int primarySize) {
        this.fontName = fontName;
        this.languages = languages;
        this.primarySize = primarySize;
    }

    public String getFontName() {
        return fontName;
    }

    public boolean isSupportedLanguage(String language) {
        return languages == null || languages.contains(language);
    }

    public int getPrimarySize() {
        return primarySize;
    }
}
