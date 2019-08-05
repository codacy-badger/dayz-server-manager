package sk.dayz.modvalidator.steam.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RuntimeServiceUtils {

    public List<File> getFilesFromProcess(Process process, String destPath) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        List<File> extractedFiles = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            File file = new File(String.format("%s/%s", destPath, line));
            extractedFiles.add(file);
        }
        log.debug("Decompression output: {}", extractedFiles);
        return extractedFiles;
    }
}
