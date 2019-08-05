package sk.dayz.modvalidator.model.ftp;

import lombok.Data;

@Data
public class FTPModDirectoryInfo {

    private long totalSize;
    private int totalDirs;
    private int totalFiles;

    @Override
    public String toString() {
        return "{totalSize=" + totalSize +
                ", totalDirs=" + totalDirs +
                ", totalFiles=" + totalFiles +
                '}';
    }
}
