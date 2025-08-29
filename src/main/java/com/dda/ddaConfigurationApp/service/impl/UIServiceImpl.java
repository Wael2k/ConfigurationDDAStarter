package com.dda.ddaConfigurationApp.service.impl;

import com.dda.ddaConfigurationApp.dto.ConfigDDAProjectDto;
import com.dda.ddaConfigurationApp.service.UIService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class UIServiceImpl implements UIService {
    @Override
    public void createProjectStructure(ConfigDDAProjectDto configDDAProjectDto) throws Exception {
        createUIFolders(configDDAProjectDto.getPathResourceUI(), configDDAProjectDto.getServiceName());
        generateI18nFile(configDDAProjectDto.getPathResourceUI(), configDDAProjectDto.getServiceName(), configDDAProjectDto.getScreenComponentsList());
        generateTemplatesPathFile(configDDAProjectDto.getPathResourceUI(), configDDAProjectDto.getServiceName(), configDDAProjectDto.getScreenComponentsList());
        generateScreenTemplates(configDDAProjectDto.getPathResourceUI(), configDDAProjectDto.getServiceName(), configDDAProjectDto.getScreenComponentsList());
    }

    @SneakyThrows
    public static void generateI18nFile(String pathResourceUI, String serviceName,
                                        List<ConfigDDAProjectDto.ScreenComponents> screenComponentsList) {
        String i18Path = pathResourceUI + "/i18n/android";
        File i18Folder = new File(i18Path);
        if (!i18Folder.exists()) i18Folder.mkdirs();

        File i18File = new File(i18Folder, "i18-" + serviceName + ".json");

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        for (int i = 0; i < screenComponentsList.size(); i++) {
            String screenName = screenComponentsList.get(i).getNameScreen();
            String key = screenName + "-template-screen";

            sb.append("  \"").append(key).append("\": {\n")
                    .append("    \"en\": {},\n")
                    .append("    \"ar\": {}\n")
                    .append("  }");

            if (i < screenComponentsList.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("}\n");

        try (FileWriter writer = new FileWriter(i18File)) {
            writer.write(sb.toString());
        }

        log.info("i18n file created: {}", i18File.getAbsolutePath());
    }

    public static void createUIFolders(String pathResourceUI, String serviceName) {
        String templatesPath = pathResourceUI + "/templates";

        File serviceFolder = new File(templatesPath, serviceName);
        if (!serviceFolder.exists()) serviceFolder.mkdirs();
        log.info("Service folder created: {}", serviceFolder.getAbsolutePath());

        File androidFolder = new File(serviceFolder, "android");
        if (!androidFolder.exists()) androidFolder.mkdirs();
        log.info("Android folder created: {}", androidFolder.getAbsolutePath());

        File latestFolder = new File(androidFolder, "latest");
        if (!latestFolder.exists()) latestFolder.mkdirs();
        log.info("Latest folder created: {}", latestFolder.getAbsolutePath());
    }

    public static void generateTemplatesPathFile(String pathResourceUI, String serviceName,
                                                 List<ConfigDDAProjectDto.ScreenComponents> screenComponentsList) throws IOException {
        String templatesPathDir = pathResourceUI + "/templates/templatespath";
        File templatesPathFolder = new File(templatesPathDir);
        if (!templatesPathFolder.exists()) templatesPathFolder.mkdirs();

        File templatesPathFile = new File(templatesPathFolder, serviceName + "-templates-path.json");

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        for (int i = 0; i < screenComponentsList.size(); i++) {
            String screenName = screenComponentsList.get(i).getNameScreen();
            String key = screenName + "-template-screen";
            String value = serviceName + "/";

            sb.append("  \"").append(key).append("\": \"").append(value).append("\"");
            if (i < screenComponentsList.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("}\n");

        try (FileWriter writer = new FileWriter(templatesPathFile)) {
            writer.write(sb.toString());
        }

        log.info("Templates path JSON created: {}", templatesPathFile.getAbsolutePath());
    }

    public static void generateScreenTemplates(String pathResourceUI, String serviceName,
                                               List<ConfigDDAProjectDto.ScreenComponents> screenComponentsList) throws IOException {
        String screensDirPath = pathResourceUI + "/templates/" + serviceName + "/android/latest";
        File screensDir = new File(screensDirPath);
        if (!screensDir.exists()) screensDir.mkdirs();

        for (ConfigDDAProjectDto.ScreenComponents screen : screenComponentsList) {
            String screenName = screen.getNameScreen();
            String fileName = screenName + "-template-screen.ftl";
            File screenFile = new File(screensDir, fileName);

            boolean hasPrevious = false;
            if (screen.getScreenSequencesList() != null) {
                hasPrevious = screen.getScreenSequencesList().stream()
                        .anyMatch(seq -> seq.getPrevious() != null && !seq.getPrevious().isEmpty());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"name\": \"").append(screenName).append("-template-screen\",\n");
            sb.append("  \"type\": \"sequence\",\n");

            if (hasPrevious) {
                sb.append("  \"backToExit\": true,\n");
            } else {
                sb.append("  \"backState\": \"LOCAL\",\n");
            }

            sb.append("\n  \"screens\": [\n");
            sb.append("    {\n");
            sb.append("      \"title\": \"${labels[\"screen.title\"]!\"???screen.title???\"}\",\n");
            sb.append("      \"name\": \"").append(screenName).append("-template-screen\",\n");
            sb.append("      \"type\": \"screen\",\n");
            sb.append("      \"menu\": [\n");
            sb.append("        {\n");
            sb.append("        }\n");
            sb.append("      ],\n");
            sb.append("      \"components\": [\n");
            sb.append("        {\n");
            sb.append("          \"type\": \"stack\",\n");
            sb.append("          \"vId\": \"groupMain\",\n");
            sb.append("          \"sticky\": true,\n");
            sb.append("          \"style\": {\n");
            sb.append("            \"layoutWidth\": \"MATCH\",\n");
            sb.append("            \"sid\": \"cardview_primary\"\n");
            sb.append("          },\n");
            sb.append("          \"components\": [\n");
            sb.append("            {\n");
            sb.append("              \"type\": \"button\",\n");
            sb.append("              \"title\": \"${labels[\"screen.button\"]!\"???screen.button???\"}\",\n");
            sb.append("              \"name\": \"next\",\n");
            sb.append("              \"action\": true,\n");
            sb.append("              \"dependencies\": []\n");
            sb.append("            }\n");
            sb.append("          ]\n");
            sb.append("        }\n");
            sb.append("      ]\n");
            sb.append("    }\n");
            sb.append("  ]\n");
            sb.append("}\n");

            try (FileWriter writer = new FileWriter(screenFile)) {
                writer.write(sb.toString());
            }
            log.info("Created screen template: {}", screenFile.getAbsolutePath());
        }
    }
}
