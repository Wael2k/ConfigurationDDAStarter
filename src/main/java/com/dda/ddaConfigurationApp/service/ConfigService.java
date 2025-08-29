package com.dda.ddaConfigurationApp.service;

import com.dda.ddaConfigurationApp.dto.ConfigDDAProjectDto;
import org.springframework.web.bind.annotation.RequestBody;

public interface ConfigService {
     void createDDAProject(@RequestBody ConfigDDAProjectDto configDDAProjectDto);
}
