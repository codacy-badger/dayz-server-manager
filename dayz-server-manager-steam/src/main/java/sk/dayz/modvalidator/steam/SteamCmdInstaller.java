package sk.dayz.modvalidator.steam;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sk.dayz.modvalidator.model.enums.OSType;
import sk.dayz.modvalidator.model.exception.BreakException;
import sk.dayz.modvalidator.model.exception.SteamException;
import sk.dayz.modvalidator.steam.decompresion.DecompressionService;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class SteamCmdInstaller {

    private final DecompressionService decompressionService;
    private final String windowsSteamCmdUrl;
    private final String linuxSteamCmdUrl;
    private final String steamCmdFolder;
    private final String linuxExecName;
    private final String winExecName;

    @Autowired
    public SteamCmdInstaller(DecompressionService decompressionService,
                             @Value("${steam.windows.url}") String windowsSteamCmdUrl,
                             @Value("${steam.linux.url}") String linuxSteamCmdUrl,
                             @Value("${steam.folderName}") String steamCmdFolder,
                             @Value("${steam.windows.exec}") String winExecName,
                             @Value("${steam.linux.exec}") String linuxExecName) {
        this.decompressionService = decompressionService;
        this.windowsSteamCmdUrl = windowsSteamCmdUrl;
        this.linuxSteamCmdUrl = linuxSteamCmdUrl;
        this.steamCmdFolder = steamCmdFolder;
        this.linuxExecName = linuxExecName;
        this.winExecName = winExecName;
    }

    public File setupSteamCmd() {
        final OSType osType = getOperatingSystemType();
        String url;

        if (osType == OSType.Other || osType == OSType.MacOS) {
            throw new SteamException("Unsupported OS Type!");
        } else if (osType == OSType.Linux) {
            url = linuxSteamCmdUrl;
        } else {
            url = windowsSteamCmdUrl;
        }

        File steamCmd = getSteamCmdIfExists();
        if (steamCmd != null) {
            log.debug("SteamCmd already initialized !");
            return steamCmd;
        }

        log.info("Installing SteamCmd...");
        String fileName = getFileName(url);
        setupSteamCmd(url, fileName);
        String destinationPath = createDestinationFolder();
        steamCmd = getSteamCmdFile(decompressionService.decompress(fileName, destinationPath));
        if (steamCmd == null) {
            throw new SteamException("SteamCmd not found!");
        }
        cleanDownloadedArchive(fileName);
        throw new BreakException("Please login to SteamCmd...");
    }

    private void cleanDownloadedArchive(String fileName) {
        new File(fileName).deleteOnExit();
    }

    private File getSteamCmdFile(List<File> files) {
        File steamCmd = null;
        for (File file : files) {
            String name = file.getName();
            if (name.equals(winExecName) || name.equals(linuxExecName)) {
                if (file.exists()) {
                    steamCmd = file;
                }
            }
        }
        return steamCmd;
    }

    private String createDestinationFolder() {
        String destinationPath = getDestinationPath();
        log.debug("Creating destination folder: {}", destinationPath);
        File destinationFolder = new File(getDestinationPath());
        boolean isCreated = destinationFolder.mkdir();
        if (! isCreated) {
            throw new SteamException("Unable to create default folder !");
        }
        log.debug("Folder {} created!", destinationPath);
        return destinationPath;
    }

    private File getSteamCmdIfExists() {
        log.debug("Check if steamCmd exists...");
        File file = new File(getDestinationPath());
        if (! file.exists()) {
            return null;
        }
        if (! file.isDirectory()) {
            return null;
        }
        File[] files = file.listFiles();
        if (files != null) {
            return getSteamCmdFile(Arrays.asList(files));
        }
        return null;
    }

    private String getDestinationPath() {
        return getAbsolutePath() + steamCmdFolder;
    }

    private String getAbsolutePath() {
        String absolutePath = new File(".").getAbsolutePath();
        if (absolutePath.endsWith(".")) {
            int length = absolutePath.length();
            absolutePath = absolutePath.substring(0, length -1);
        }
        return absolutePath;
    }

    private String getFileName(String url) {
        final int index = url.lastIndexOf("/");
        return url.substring(index + 1);
    }

    private void setupSteamCmd(String url, String fileName) {
        try {
            log.info("Started to downloading of SteamCmd...");
            URL website = new URL(url);
            ReadableByteChannel readableByteChannel = Channels.newChannel(website.openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            log.info("SteamCmd successfully downloaded!");
        } catch (IOException e) {
            throw new SteamException("Failed to setup steamCmd", e);
        }
    }

    private OSType getOperatingSystemType() {
        log.debug("Determining OS Type ...");
        final String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((OS.contains("mac")) || (OS.contains("darwin"))) {
            log.debug("OS: {}", OSType.MacOS);
            return OSType.MacOS;
        } else if (OS.contains("win")) {
            log.debug("OS: {}", OSType.Windows);
            return OSType.Windows;
        } else if (OS.contains("nux")) {
            log.debug("OS: {}", OSType.Linux);
            return OSType.Linux;
        } else {
            log.debug("OS: {}", OSType.Other);
            return OSType.Other;
        }
    }
}
