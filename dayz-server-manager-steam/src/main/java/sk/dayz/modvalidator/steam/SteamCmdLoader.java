package sk.dayz.modvalidator.steam;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sk.dayz.modvalidator.model.exception.SteamException;
import sk.dayz.modvalidator.model.steam.SteamMod;

import java.io.*;
import java.util.Calendar;
import java.util.Scanner;

@Slf4j
@Component
public class SteamCmdLoader {

    private final SteamCmdInstaller steamCmdInstaller;
    private final String[] acceptedFpsValues;
    private final long dayzAppId;

    @Autowired
    public SteamCmdLoader(SteamCmdInstaller steamCmdInstaller, @Value("${dayz.appId}") long dayzAppId,
                          @Value("${ftp.acceptedFiles}") String[] acceptedFpsValues) {
        this.steamCmdInstaller = steamCmdInstaller;
        this.acceptedFpsValues = acceptedFpsValues;
        this.dayzAppId = dayzAppId;
    }

    public SteamMod getSteamModInformation(String workshopId) {
        log.info("Constructing Steam Mod information ...");
        final File mod = getLocalMod(workshopId);
        SteamMod steamMod = new SteamMod();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mod.lastModified());
        steamMod.setLastModified(calendar);
        steamMod.setModLocation(mod.getAbsolutePath());
        steamMod.setWorkshopId(mod.getName());
        steamMod.setSize(getTotalSize(mod));

        log.info("Steam mod information: {}", steamMod);
        return steamMod;
    }

    public void updateMod(String username, String workshopId) {
        log.info("Updating Mod ID: {}", workshopId);
        String output = runCommand(username, workshopId, String.format("workshop_download_item %s %s", dayzAppId,
                workshopId));
        checkForErrors(output);
    }

    public void initializeMod(String username, String workshopId) {
        log.info("Downloading Mod ID: {}", workshopId);
        String output = runCommand(username, workshopId, String.format("workshop_download_item %s %s", dayzAppId,
                workshopId));
        checkForErrors(output);
    }

    public boolean checkExistenceOfMod(String workshopId) {
        log.info("Checking presence of local mod: {}", workshopId);
        try {
            getLocalMod(workshopId);
            log.info("Mod {} was already downloaded!", workshopId);
            return true;
        } catch (SteamException e) {
            log.info("Mod {} is not downloaded !", workshopId);
            return false;
        }
    }

    private long getTotalSize(File mod) {
        File[] files = mod.listFiles();
        long totalSize = 0;
        if (files != null && files.length != 0) {
            for (File file : files) {
                boolean acceptedValue = checkAcceptedValue(file.getName());
                if (acceptedValue) {
                    if (file.isDirectory()) {
                        totalSize += FileUtils.sizeOfDirectory(file);
                    } else {
                        totalSize += file.length();
                    }
                }
            }
            return totalSize;
        }
        throw new SteamException("Failed to calculate mod size!");
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

    private File getLocalMod(String workshopId) {
        File file = new File(".");
        File modsFolder = getFolder(file, "steam");
        File steamapps = getFolder(modsFolder, "steamapps");
        File workshop = getFolder(steamapps, "workshop");
        File content = getFolder(workshop, "content");
        File dayzApp = getFolder(content, Long.toString(dayzAppId));
        return getFolder(dayzApp, workshopId);
    }

    private String runCommand(String username, String workshopId, String... command) {
        File steamCmdScript = null;
        try {
            File steamCmd = steamCmdInstaller.setupSteamCmd();
            log.debug("SteamCmd Location: {}", steamCmd);

            steamCmdScript = prepareSteamCmdScriptFile(workshopId, username, command);
            Process process = runScriptWithSteamCmd(steamCmd, steamCmdScript);
            return getCommandOutput(process);
        } catch (IOException | InterruptedException e) {
            throw new SteamException("Failed to execute steam cmd commands!", e);
        } finally {
            if (steamCmdScript != null) {
                steamCmdScript.deleteOnExit();
            }
        }
    }

    private void checkForErrors(String output) {
        if (output.contains("FAILED")) {
            throw new SteamException("Failed to execute command!");
        }
    }

    private String getCommandOutput(Process process) throws InterruptedException {
        SequenceInputStream sequenceInputStream = new SequenceInputStream(process.getInputStream(),
                process.getErrorStream());
        String cmdOutput = readOutput(sequenceInputStream);
        log.info("SteamCmd Output:\n{}", cmdOutput);
        process.waitFor();
        return cmdOutput;
    }

    private Process runScriptWithSteamCmd(File steamCmd, File steamCmdScript) throws IOException {
        log.debug("Executing script ...");
        return Runtime.getRuntime().exec(String.format("%s +runscript %s", steamCmd,
                steamCmdScript.getAbsolutePath()), null, steamCmd.getParentFile());
    }

    private File prepareSteamCmdScriptFile(String workshopId, String username, String... commands) {
        log.debug("Preparing SteamCmd scrip ...");
        try {
            File steamCmdScript = new File("/mod" + workshopId + "script.txt");
            FileWriter fileWriter = new FileWriter(steamCmdScript);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("@ShutdownOnFailedCommand 1");
            printWriter.println("@NoPromptForPassword 1");
            printWriter.println(String.format("login %s", username));
            for (String command : commands) {
                printWriter.println(command);
            }
            printWriter.println("quit");
            printWriter.close();
            log.debug("SteamCmd script constructed...");
            log.debug("Script location: {}", steamCmdScript.getAbsolutePath());
            return steamCmdScript;
        } catch (IOException e) {
            throw new SteamException("Failed to construct steam script!", e);
        }
    }

    private String readOutput(SequenceInputStream sequenceInputStream) throws InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();
        int maxCount = 10;
        int count = 0;
        while (true) {
            Scanner scanner = new Scanner(sequenceInputStream);
            if (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line == null) {
                    break;
                }
                stringBuilder.append(line).append("\n");
//                Thread.sleep(100);
            } else {
                Thread.sleep(100);
                count++;
                if (maxCount == count) {
                    break;
                }
            }
        }
        return stringBuilder.toString();
    }

    private File getFolder(File file, String folderName) {
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            for (File folder : files) {
                if (folder.getName().equalsIgnoreCase(folderName)) {
                    return folder;
                }
            }
        }
        throw new SteamException(String.format("File %s not found !", folderName));
    }

    //TODO Rework
    // Output to file ./steamcmd.exe +runscript test.txt | Out-File -FilePath ./output.txt
    // Command ./steamcmd.sh +runscript test.txt > output.txt
    public void checkWorkshopStatus(String username) {
        log.info("Checking workshop status...");
        String output = runCommand(username, "dummyWorkshopId",
                String.format("workshop_download_item %s %s", dayzAppId, "1559212036"),
                String.format("workshop_status %s", dayzAppId));
        checkForErrors(output);
    }
}
