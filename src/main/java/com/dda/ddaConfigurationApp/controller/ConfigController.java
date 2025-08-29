package com.dda.ddaConfigurationApp.controller;

import com.dda.ddaConfigurationApp.dto.ConfigDDAProjectDto;
import com.dda.ddaConfigurationApp.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    @Autowired
    ConfigService configService;
    @PostMapping("")
    public ResponseEntity<Void> config(@RequestBody ConfigDDAProjectDto configDDAProjectDto) {
        configService.createDDAProject(configDDAProjectDto);
        return ResponseEntity.ok().build();
    }// Rama was here ; samer was here

}
