package sk.dayz.modvalidator.service.result;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import sk.dayz.modvalidator.model.Mod;
import sk.dayz.modvalidator.model.exception.ResultException;
import sk.dayz.modvalidator.model.ftp.FTPMod;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class ResultService {

    public void createResult(List<Mod> modsForUpdating) {
        File resultFolder = createResultFolder();
        for (Mod mod : modsForUpdating) {
            try {
                log.info("Copy {} mod to result folder ...", mod.getFtpMod().getModInfo().getModOriginalName());
                FileUtils.copyDirectory(new File(mod.getSteamMod().getModLocation()), createFile(resultFolder,
                        mod.getFtpMod().getFtpName()));
            } catch (IOException e) {
                throw new ResultException("Failed to copy mod for updating!", e);
            }
        }
    }

    private File createFile(File resultFolder, String fileName) {
        return new File(String.format("%s/%s", resultFolder.getAbsolutePath(), fileName));
    }

    private File createResultFolder() {
        File resultFolder = new File(String.format("%s/result", new File(".").getAbsolutePath()));
        if (! resultFolder.exists()) {
            if (resultFolder.mkdir()) {
                log.info("Result directory created!");
            }
        }
        clearResultFolder(resultFolder);
        return resultFolder;
    }

    private void clearResultFolder(File resultFolder) {
        File[] files = resultFolder.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                file.deleteOnExit();
            }
        }
    }
}
