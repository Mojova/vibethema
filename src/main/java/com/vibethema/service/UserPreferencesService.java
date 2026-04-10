package com.vibethema.service;

import java.util.prefs.Preferences;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Service to manage and persist user-specific application preferences.
 */
public class UserPreferencesService {
    private static final String PREF_NODE = "com.vibethema";
    private static final String KEY_PAPER_SIZE = "paper_size";
    
    public static final String PAPER_SIZE_A4 = "A4";
    public static final String PAPER_SIZE_LETTER = "Letter";

    private final Preferences prefs;
    private static UserPreferencesService instance;

    private UserPreferencesService() {
        this.prefs = Preferences.userRoot().node(PREF_NODE);
    }

    public static synchronized UserPreferencesService getInstance() {
        if (instance == null) {
            instance = new UserPreferencesService();
        }
        return instance;
    }

    public String getPaperSize() {
        return prefs.get(KEY_PAPER_SIZE, PAPER_SIZE_A4);
    }

    public void setPaperSize(String size) {
        prefs.put(KEY_PAPER_SIZE, size);
    }

    public PDRectangle getPDRectangle() {
        String size = getPaperSize();
        if (PAPER_SIZE_LETTER.equalsIgnoreCase(size)) {
            return PDRectangle.LETTER;
        }
        return PDRectangle.A4;
    }
}
