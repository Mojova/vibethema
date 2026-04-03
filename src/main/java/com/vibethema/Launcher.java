package com.vibethema;

import java.awt.Desktop;

public class Launcher {
    public static void main(String[] args) {
        // Handle macOS double-click events
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
                    desktop.setOpenFileHandler(e -> {
                        if (!e.getFiles().isEmpty()) {
                            Main.setPendingFile(e.getFiles().get(0));
                        }
                    });
                }
            } catch (Exception e) {
                // Ignore if not supported on this platform
            }
        }

        // Handle Windows/Linux command-line arguments
        if (args.length > 0) {
            Main.setPendingFile(new java.io.File(args[0]));
        }

        Main.main(args);
    }
}
