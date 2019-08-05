package sk.dayz.modvalidator.service.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sk.dayz.modvalidator.model.configuration.DayzConfiguration;
import sk.dayz.modvalidator.model.exception.ConfigurationException;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class DayzConfigurationService {

    private final String dayzConfigurationName;
    private final ObjectMapper objectMapper;

    @Autowired
    public DayzConfigurationService(ObjectMapper objectMapper,
                                    @Value("${dayz.configuration.name}") String dayzConfigurationName) {
        this.dayzConfigurationName = dayzConfigurationName;
        this.objectMapper = objectMapper;
    }

    public DayzConfiguration loadConfiguration() {
        log.info("Getting configuration file {} ...", dayzConfigurationName);
        File cfgFile = new File(dayzConfigurationName);
        if (! cfgFile.exists()) {
            log.info("Configuration {} not exist creating default configuration ...", dayzConfigurationName);
            throw new ConfigurationException("Configuration not exist!");
        }
        DayzConfiguration dayzConfiguration;
        try {
            dayzConfiguration = objectMapper.readValue(cfgFile, DayzConfiguration.class);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to deserialize configuration!", e);
        }
        if (dayzConfiguration == null) {
            throw new ConfigurationException("Invalid configuration file!");
        }
        log.info("Configuration: {}", dayzConfiguration.toString());
        return dayzConfiguration;
    }
}
