package omnivoxel.common.settings;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// TODO: Make static
public final class Settings {
    private final List<Setting> settings;

    public Settings() {
        settings = new ArrayList<>();
    }

    public void load(String configLocation) throws IOException {
        Files.createDirectories(Path.of(configLocation));
        boolean newSettings = new File(configLocation + "/settings").createNewFile();
        if (newSettings) {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(configLocation + "/settings"));
            bufferedOutputStream.write(ConstantClientSettings.DEFAULT_SETTING_CONTENTS.getBytes());
            bufferedOutputStream.flush();
        }
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(configLocation + "/settings"));
        byte[] settingBytes = bufferedInputStream.readAllBytes();
        StringBuilder settingFileContents = new StringBuilder();
        for (byte b : settingBytes) {
            settingFileContents.append((char) b);
        }
        String[] settings = settingFileContents.toString().split("[\n\r]");
        this.settings.clear();
        for (String setting : settings) {
            if (setting.contains("=")) {
                String[] keyValue = setting.split("=");
                this.settings.add(new Setting(keyValue[0], keyValue[1]));
            }
        }
    }

    public String getSetting(String settingName) {
        for (Setting setting : settings) {
            if (Objects.equals(setting.key(), settingName)) {
                return setting.value();
            }
        }
        return null;
    }

    public String getSetting(String settingName, String defaultValue) {
        String setting = getSetting(settingName);
        if (setting == null) {
            return defaultValue;
        }
        return setting;
    }

    public int getIntSetting(String settingName, int defaultValue) {
        String setting = getSetting(settingName);
        if (setting == null) {
            return defaultValue;
        }
        return Integer.parseInt(setting);
    }

    public float getFloatSetting(String settingName, float defaultValue) {
        String setting = getSetting(settingName);
        if (setting == null) {
            return defaultValue;
        }
        return Float.parseFloat(setting);
    }
}