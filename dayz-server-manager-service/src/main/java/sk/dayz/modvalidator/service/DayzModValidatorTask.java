package sk.dayz.modvalidator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sk.dayz.modvalidator.model.Mod;
import sk.dayz.modvalidator.model.configuration.DayzConfiguration;
import sk.dayz.modvalidator.model.exception.BreakException;
import sk.dayz.modvalidator.model.ftp.FTPMod;
import sk.dayz.modvalidator.model.steam.SteamMod;
import sk.dayz.modvalidator.service.configuration.DayzConfigurationService;
import sk.dayz.modvalidator.service.mods.FtpModsInformationService;
import sk.dayz.modvalidator.service.result.ResultService;
import sk.dayz.modvalidator.steam.SteamCmdLoader;
import sk.dayz.modvalidator.service.diff.SteamDifferenceCalculatorService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DayzModValidatorTask {

    private final SteamDifferenceCalculatorService steamDifferenceCalculatorService;
    private final FtpModsInformationService ftpModsInformationService;
    private final DayzConfigurationService dayzConfigurationService;
    private final SteamCmdLoader steamCmdLoader;
    private final ResultService resultService;
    private final boolean updatingEnabled;

    private boolean running = false;

    @Autowired
    public DayzModValidatorTask(SteamCmdLoader steamCmdLoader, FtpModsInformationService ftpModsInformationService,
                                SteamDifferenceCalculatorService steamDifferenceCalculatorService,
                                DayzConfigurationService dayzConfigurationService, ResultService resultService,
                                @Value("${mods.updatingEnabled}") boolean updatingEnabled) {
        this.steamDifferenceCalculatorService = steamDifferenceCalculatorService;
        this.ftpModsInformationService = ftpModsInformationService;
        this.dayzConfigurationService = dayzConfigurationService;
        this.updatingEnabled = updatingEnabled;
        this.steamCmdLoader = steamCmdLoader;
        this.resultService = resultService;
    }

    @Scheduled(initialDelay = 0, fixedRateString = "${mods.interval}")
    public void checkMods() {
        if (updatingEnabled) {
            if (! running) {
                log.info("Checking Dayz mods ...");
                try {
                    DayzConfiguration dayzConfiguration = dayzConfigurationService.loadConfiguration();
                    List<FTPMod> serverMods = ftpModsInformationService.loadMods(dayzConfiguration);
                    List<Mod> modsForUpdating = new ArrayList<>();

                    String username = dayzConfiguration.getSteam().getUsername();

                    // TODO Check fo updates
                    // steamCmdLoader.checkWorkshopStatus(username);

                    for (FTPMod ftpMod : serverMods) {
                        String modId = ftpMod.getModInfo().getModId();
                        log.info("Start to checking Mod: {}, workshopId: {}", ftpMod.getModInfo().getModOriginalName(),
                                ftpMod.getModInfo().getModId());
                        if (! steamCmdLoader.checkExistenceOfMod(modId)) {
                            steamCmdLoader.initializeMod(username, modId);
                        }

                        SteamMod steamMod = steamCmdLoader.getSteamModInformation(modId);
                        boolean updateNeeded = steamDifferenceCalculatorService.isUpdateNeeded(steamMod, ftpMod);
                        if (updateNeeded) {
                            steamCmdLoader.updateMod(username, modId);
                            modsForUpdating.add(Mod.builder().ftpMod(ftpMod).steamMod(steamMod).build());
                        }
                    }

                    if (!modsForUpdating.isEmpty()) {
                        log.info("Result: {}", modsForUpdating);
                        log.info("Creating result ... !");
                        resultService.createResult(modsForUpdating);
                    } else {
                        log.info("All mods are up to date ...");
                    }
                } catch (BreakException e) {
                    log.info("You are not logged in SteamCmd!");
                    log.info("Please login to SteamCmd for the first time, then I will check you mods in next interval...");
                } finally {
                    running = false;
                }
            }
        }
    }
}
