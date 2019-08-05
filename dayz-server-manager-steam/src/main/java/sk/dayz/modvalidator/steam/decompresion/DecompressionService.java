package sk.dayz.modvalidator.steam.decompresion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sk.dayz.modvalidator.model.exception.DecompressionException;
import sk.dayz.modvalidator.steam.utils.RuntimeServiceUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class DecompressionService {

    private final RuntimeServiceUtils runtimeServiceUtils;

    @Autowired
    public DecompressionService(RuntimeServiceUtils runtimeServiceUtils) {
        this.runtimeServiceUtils = runtimeServiceUtils;
    }

    public List<File> decompress(String fileName, String destPath) {
        if (fileName.contains("zip")) {
            return unzip(fileName, destPath);
        } else if (fileName.contains("tar.gz")) {
            return unTar(fileName, destPath);
        } else {
            throw new DecompressionException("Bad format for decompression!");
        }
    }

    private List<File> unzip(String fileName, String destPath) {
        log.debug("Starting to decompress: {}", fileName);
        List<File> extractedFiles = new ArrayList<>();
        byte[] buffer = new byte[1024];

        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(fileName));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(new File(destPath), zipEntry.getName());
                FileOutputStream fileOutputStream = new FileOutputStream(newFile);
                int len;
                while ((len = zipInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, len);
                }
                fileOutputStream.close();
                extractedFiles.add(newFile);
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
            log.debug("Decompression output: {}", extractedFiles);
            return extractedFiles;
        } catch (IOException e) {
            throw new DecompressionException("Failed to decompress file !", e);
        }
    }

    private List<File> unTar(String fileName, String destPath) {
        try {
            log.debug("Starting to decompress: {}", fileName);

            Runtime runtime = Runtime.getRuntime();

            Process tarProcess = runtime.exec(String.format("tar -xzf %s -C %s", fileName, destPath));
            tarProcess.waitFor();

            Process listProcess = runtime.exec(String.format("ls %s", destPath));
            listProcess.waitFor();

            return runtimeServiceUtils.getFilesFromProcess(listProcess, destPath);
        } catch (IOException|InterruptedException e) {
            throw new DecompressionException("Failed to decompress file !", e);
        }
    }

    private static File newFile(File destinationDir, String name) throws IOException {
        File destFile = new File(destinationDir, name);
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new DecompressionException("Entry is outside of the target dir: " + name);
        }
        return destFile;
    }
}
