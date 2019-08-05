package sk.dayz.modvalidator.service.diff;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sk.dayz.modvalidator.model.ftp.FTPMod;
import sk.dayz.modvalidator.model.steam.SteamMod;

@Slf4j
@Component
public class SteamDifferenceCalculatorService {

    public boolean isUpdateNeeded(SteamMod steamMod, FTPMod ftpMod) {
        if (ftpMod.getInfo().getTotalSize() == steamMod.getSize()) {
            log.info("Steam Mod: {}, WorkshopId: {} is up to date!", ftpMod.getModInfo().getModOriginalName(),
                    ftpMod.getModInfo().getModId());
            return false;
        }
        log.warn("Steam Mod: {}, WorkshopId: {} is not up to date!", ftpMod.getModInfo().getModOriginalName(),
                ftpMod.getModInfo().getModId());
        return true;
    }
}
