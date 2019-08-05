package sk.dayz.modvalidator.model.ftp;

import lombok.Data;

import java.util.Calendar;

@Data
public class FTPMod {

    private String ftpName;
    private Calendar lastUpdated;
    private FTPModDirectoryInfo info;
    private ModInfo modInfo;

    @Override
    public String toString() {
        return "{ftpName='" + ftpName + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", info=" + info.toString() +
                ", modInfo=" + modInfo.toString() +
                '}';
    }
}
