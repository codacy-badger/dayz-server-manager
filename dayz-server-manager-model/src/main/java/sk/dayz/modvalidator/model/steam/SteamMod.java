package sk.dayz.modvalidator.model.steam;

import lombok.Data;

import java.util.Calendar;

@Data
public class SteamMod {

    private String workshopId;
    private Calendar lastModified;
    private String modLocation;
    // Size of addons and keys
    private long size;
}
