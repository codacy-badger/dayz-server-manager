package sk.dayz.modvalidator.model.ftp;

import lombok.Data;

@Data
public class ModInfo {

    private String modId;
    private String modOriginalName;

    @Override
    public String toString() {
        return "ModInfo{" +
                "modId='" + modId + '\'' +
                ", modOriginalName='" + modOriginalName + '\'' +
                '}';
    }
}
