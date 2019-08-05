package sk.dayz.modvalidator.service.mods;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sk.dayz.modvalidator.ftp.FtpConnector;
import sk.dayz.modvalidator.model.configuration.DayzConfiguration;
import sk.dayz.modvalidator.model.configuration.ftp.FtpConfiguration;
import sk.dayz.modvalidator.model.exception.FTPException;
import sk.dayz.modvalidator.model.ftp.FTPMod;
import sk.dayz.modvalidator.model.ftp.FTPModDirectoryInfo;
import sk.dayz.modvalidator.model.ftp.ModInfo;

import java.io.*;
import java.util.*;

@Slf4j
@Component
public class FtpModsInformationService {

    private static final String META_CPP = "meta.cpp";

    private final String[] acceptedFpsValues;
    private final FtpConnector ftpConnector;

    @Autowired
    public FtpModsInformationService(FtpConnector ftpConnector, @Value("${ftp.acceptedFiles}") String[] acceptedFpsValues) {
        this.acceptedFpsValues = acceptedFpsValues;
        this.ftpConnector = ftpConnector;
    }

    public List<FTPMod> loadMods(DayzConfiguration dayzConfiguration) {
        FTPClient ftp = null;
        try {
            ftp = connectToFtp(dayzConfiguration);
            return getModsFromFtp(ftp);
        } finally {
            disconnect(ftp);
        }
    }

    private List<FTPMod> getModsFromFtp(FTPClient ftp) {
        log.info("Getting mods from FTP ...");
        List<FTPMod> mods = new ArrayList<>();
        FTPFile[] directoriesInRoot = ftpConnector.getDirectoriesInRoot(ftp);
        for (FTPFile ftpFile : directoriesInRoot) {
            if (ftpFile.getName().contains("@")) {
                FTPMod ftpMod = new FTPMod();
                ftpMod.setFtpName(ftpFile.getName());
                ftpMod.setLastUpdated(ftpFile.getTimestamp());
                ftpMod.setInfo(calculateDirectoryInfo(ftp, "/", String.format("/%s", ftpFile.getName())));
                ftpMod.setModInfo(getModInfo(ftp, ftpFile.getName()));
                log.info("Mod Information: {}", ftpMod.toString());
                mods.add(ftpMod);
            }
        }
        return mods;
    }

    private ModInfo getModInfo(FTPClient ftp, String modName) {
        ModInfo modInfo = new ModInfo();
        File tempFile = null;
        try {
            FTPFile[] subFiles = ftp.listFiles(String.format("/%s/", modName));
            checkIfMetaInfoExists(subFiles);

            tempFile = File.createTempFile(modName, META_CPP);
            downloadMetaInfo(ftp, modName, tempFile);

            List<String> lines = readMetaFile(tempFile);
            Map<String, String> metaMap = parseMetaFile(lines);

            modInfo.setModId(metaMap.get("publishedid"));
            modInfo.setModOriginalName(metaMap.get("name"));
            return modInfo;
        } catch (IOException e) {
            throw new FTPException("Failed to get mod information !", e);
        } finally {
            if (tempFile != null) {
                tempFile.deleteOnExit();
            }
        }
    }

    private Map<String, String> parseMetaFile(List<String> lines) {
        Map<String, String> info = new HashMap<>();
        for(String line : lines) {
            line = line.replace(";", "");
            String[] split = line.split("=");
            info.put(split[0].replaceAll(" ", ""), split[1].replaceAll(" ", ""));
        }
        return info;
    }

    private void downloadMetaInfo(FTPClient ftp, String modName, File tempFile) throws IOException {
        log.info("Downloading meta info for mod: {}", modName);
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
        boolean success = ftp.retrieveFile(String.format("/%s/%s", modName, META_CPP), outputStream);
        outputStream.close();
        if (! success) {
            throw new FTPException("Failed to download meta.cpp from FTP mod directory!");
        }
        log.info("File {} has been downloaded successfully.", META_CPP);
    }

    private void checkIfMetaInfoExists(FTPFile[] subFiles) {
        FTPFile metaInfo = getMetaInfo(subFiles);
        if (metaInfo == null) {
            throw new FTPException("Missing meta.cpp in FTP mod directory!");
        }
    }

    private List<String> readMetaFile(File tempFile) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(tempFile));
        String line;
        List<String> lines = new ArrayList<>();
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    private FTPFile getMetaInfo(FTPFile[] subFiles) {
        FTPFile ftpFile = null;
        for (FTPFile file : subFiles) {
            if (file.getName().equals(META_CPP)) {
                ftpFile = file;
            }
        }
        return ftpFile;
    }

    private FTPModDirectoryInfo calculateDirectoryInfo(FTPClient ftpClient, String parentDir, String currentDir) {
        long totalSize = 0;
        int totalDirs = 0;
        int totalFiles = 0;

        String directoryToList = parentDir;
        if (!currentDir.equals("")) {
            directoryToList += "/" + currentDir;
        }

        try {
            FTPFile[] subFiles = ftpClient.listFiles(directoryToList);
            if (subFiles != null && subFiles.length > 0) {
                for (FTPFile ftpFile : subFiles) {
                    String currentFileName = ftpFile.getName();
                    if (currentFileName.equals(".") || currentFileName.equals("..")) {
                        // skip parent directory and the directory itself
                        continue;
                    }

                    if (! checkAcceptedValue(currentFileName)) {
                        continue;
                    }

                    if (ftpFile.isDirectory()) {
                        totalDirs++;
                        FTPModDirectoryInfo subDirInfo =
                                calculateDirectoryInfo(ftpClient, directoryToList, currentFileName);
                        totalDirs += subDirInfo.getTotalDirs();
                        totalFiles += subDirInfo.getTotalFiles();
                        totalSize += subDirInfo.getTotalSize();
                    } else {
                        totalSize += ftpFile.getSize();
                        totalFiles++;
                    }
                }
            }

            FTPModDirectoryInfo ftpModDirectoryInfos = new FTPModDirectoryInfo();
            ftpModDirectoryInfos.setTotalDirs(totalDirs);
            ftpModDirectoryInfos.setTotalFiles(totalFiles);
            ftpModDirectoryInfos.setTotalSize(totalSize);

            return ftpModDirectoryInfos;
        } catch (IOException e) {
            throw new FTPException("Failed to calculate mod directory information!", e);
        }
    }

    private boolean checkAcceptedValue(String currentFileName) {
        boolean accepted = false;
        for (String acceptedFpsValue: acceptedFpsValues) {
            if (currentFileName.contains(acceptedFpsValue)) {
                accepted = true;
                break;
            }
        }
        return accepted;
    }

    private FTPClient connectToFtp(DayzConfiguration dayzConfiguration) {
        FtpConfiguration ftp = dayzConfiguration.getFtp();
        return ftpConnector.connect(ftp.getHost(), ftp.getPort(), ftp.getUsername(), ftp.getPassword());
    }

    private void disconnect(FTPClient ftp) {
        if (ftp != null) {
            try {
                ftp.disconnect();
            } catch (IOException e) {
                log.warn("Failed to disconnect from FTP !");
            }
        }
    }
}
