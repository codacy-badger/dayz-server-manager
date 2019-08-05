package sk.dayz.modvalidator.model;

import lombok.Builder;
import lombok.Data;
import sk.dayz.modvalidator.model.ftp.FTPMod;
import sk.dayz.modvalidator.model.steam.SteamMod;

@Data
@Builder
public class Mod {

    private SteamMod steamMod;
    private FTPMod ftpMod;
}
