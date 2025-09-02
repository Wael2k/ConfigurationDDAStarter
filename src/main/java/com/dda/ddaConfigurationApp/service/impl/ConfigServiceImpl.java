package com.dda.ddaConfigurationApp.service.impl;

import com.dda.ddaConfigurationApp.dto.ConfigDDAProjectDto;
import com.dda.ddaConfigurationApp.service.ConfigService;
import com.dda.ddaConfigurationApp.service.CoreService;
import com.dda.ddaConfigurationApp.service.UIService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {
    @Autowired
    private CoreService coreService;
    @Autowired
    private UIService uiService;
    @Override
    @SneakyThrows
    public void createDDAProject(ConfigDDAProjectDto configDDAProjectDto) {
        log.info("configDDAProjectDto={}", configDDAProjectDto);
        coreService.createProjectStructure(configDDAProjectDto);
         uiService.createProjectStructure(configDDAProjectDto);

    }
}
