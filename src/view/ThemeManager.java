package view;

import com.formdev.flatlaf.*;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ThemeManager {

    private static boolean darkModeActive;

    public static void setupSystemTheme() {
    	darkModeActive = detectSystemDarkMode();
    	applyTheme();
    }

    public static void toggleTheme() {
        darkModeActive = !darkModeActive;
        applyTheme();
    }

    private static void applyTheme() {
        if (darkModeActive) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
        }
    }

    public static boolean isDarkMode() {
        return darkModeActive;
    }

    private static boolean detectSystemDarkMode() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return isWindowsDarkMode();
        } else if (os.contains("mac")) {
            return isMacDarkMode();
        }
        return false;
    }

    private static boolean isWindowsDarkMode() {
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

    private static boolean isMacDarkMode() {
        try {
            Process process = Runtime.getRuntime().exec(
                new String[]{"defaults", "read", "-g", "AppleInterfaceStyle"}
            );
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = br.readLine();
                return line != null && line.trim().equalsIgnoreCase("Dark");
            }
        } catch (IOException ignored) {}
        return false;
    }
}
