package sk.dayz.modvalidator.model.configuration;

import lombok.Data;
import sk.dayz.modvalidator.model.configuration.ftp.FtpConfiguration;
import sk.dayz.modvalidator.model.configuration.steam.SteamConfiguration;

@Data
public class DayzConfiguration {

    private FtpConfiguration ftp;
    private SteamConfiguration steam;
}
