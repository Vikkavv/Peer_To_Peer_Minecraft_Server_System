package view;

import com.formdev.flatlaf.*;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ThemeManager {

    public static void setupSystemTheme() {
    	if (isWindowsDarkMode()) {
    	    FlatDarkLaf.setup();
    	} else {
    	    FlatLightLaf.setup();
    	}
    	for (Window w : Window.getWindows()) {
    	    SwingUtilities.updateComponentTreeUI(w);
    	    w.pack();
    	}
    }
    
    public static boolean isWindowsDarkMode() {
        try {
            Process process = Runtime.getRuntime().exec(
                "reg query HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize /v AppsUseLightTheme"
            );

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("AppsUseLightTheme")) {
                        return line.trim().endsWith("0x0"); // 0 → dark, 1 → light
                    }
                }
            }
        } catch (IOException ignored) {}
        return false;
    }
}
