package com.dda.ddaConfigurationApp.service.impl;

import com.dda.ddaConfigurationApp.dto.ConfigDDAProjectDto;
import com.dda.ddaConfigurationApp.service.CoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CoreServiceImpl implements CoreService {
    private final List<String> standardFolders = Arrays.asList(
            "business", "constants", "controller", "dto", "service", "utils"
    );

    @Override
    public void createProjectStructure(ConfigDDAProjectDto configDDAProjectDto) throws Exception {
        // Création Core
        createServiceFolders(configDDAProjectDto.getPathCore(), configDDAProjectDto.getServiceName());
        generateController(
                configDDAProjectDto.getServiceName(),
                configDDAProjectDto.getJourneyType(),
                configDDAProjectDto.getPathCore(),
                configDDAProjectDto.getUrlRestController(),
                configDDAProjectDto.getUrlEndPoint()
        );
        generateJsonFiles(
                configDDAProjectDto.getPathResourceCore(),
                configDDAProjectDto.getServiceName(),
                configDDAProjectDto.getScreenComponentsList()
        );
        generateConstantsFile(
                configDDAProjectDto.getPathCore(),
                configDDAProjectDto.getServiceName(),
                configDDAProjectDto.getJourneyType(),
                configDDAProjectDto.getScreenComponentsList()
        );
        generateServices(
                configDDAProjectDto.getPathCore(),
                configDDAProjectDto.getServiceName(),
                configDDAProjectDto.getJourneyType(),
                configDDAProjectDto.getScreenComponentsList()
        );
        updateJourneyTypeEnum(
                configDDAProjectDto.getPathCore(),
                configDDAProjectDto.getServiceName(),
                configDDAProjectDto.getJourneyType()
        );
    }

    private void createServiceFolders(String basePath, String serviceName) throws Exception {
        if (basePath == null || serviceName == null) {
            throw new Exception("Base path or service name cannot be null");
        }

        File serviceFolder = new File(basePath + File.separator + "services" + File.separator + serviceName);
        if (!serviceFolder.exists()) {
            if (!serviceFolder.mkdirs()) {
                throw new Exception("Failed to create main service folder: " + serviceFolder.getAbsolutePath());
            }
        }

        // Crée les sous-dossiers standard
        for (String folder : standardFolders) {
            File subFolder = new File(serviceFolder, folder);
            if (!subFolder.exists()) {
                if (!subFolder.mkdirs()) {
                    throw new Exception("Failed to create sub-folder: " + subFolder.getAbsolutePath());
                }
            }
            if ("business".equals(folder)) {
                createBusinessFiles(serviceFolder, serviceName);
            }
        }
    }

    private void createBusinessFiles(File serviceFolder, String serviceName) throws IOException {
        String journeyName = toCamelCase(serviceName);

        File businessFolder = new File(serviceFolder, "business");
        File businessImplFolder = new File(businessFolder, "businessImpl");
        if (!businessImplFolder.exists()) {
            if (!businessImplFolder.mkdirs()) {
                throw new IOException("Failed to create businessImpl folder");
            }
        }
        // === Interfaces ===
        File clientInterface = new File(businessFolder, journeyName + "Client.java");
        writeToFile(clientInterface,
                "package ae.gov.sdg.ext.rta.core.services." + serviceName.toLowerCase() + ".business;\n\n" +
                        "public interface " + journeyName + "Client {\n\n}");

        File businessInterface = new File(businessFolder, journeyName + "Business.java");
        writeToFile(businessInterface,
                "package ae.gov.sdg.ext.rta.core.services." + serviceName.toLowerCase() + ".business;\n\n" +
                        "public interface " + journeyName + "Business {\n\n}");

        // === Implementations ===
        File clientImpl = new File(businessImplFolder, journeyName + "ClientImpl.java");
        writeToFile(clientImpl,
                "package ae.gov.sdg.ext.rta.core.services." + serviceName.toLowerCase() + ".business.businessImpl;\n\n" +
                        "import org.springframework.stereotype.Service;\n" +
                        "import ae.gov.sdg.ext.rta.core.services." + serviceName.toLowerCase() + ".business." + journeyName + "Client;\n\n" +
                        "@Service\n" +
                        "public class " + journeyName + "ClientImpl implements " + journeyName + "Client {\n\n}");

        File businessImpl = new File(businessImplFolder, journeyName + "BusinessImpl.java");
        writeToFile(businessImpl,
                "package ae.gov.sdg.ext.rta.core.services." + serviceName.toLowerCase() + ".business.businessImpl;\n\n" +
                        "import org.springframework.stereotype.Service;\n" +
                        "import ae.gov.sdg.ext.rta.core.services." + serviceName.toLowerCase() + ".business." + journeyName + "Business;\n\n" +
                        "@Service\n" +
                        "public class " + journeyName + "BusinessImpl implements " + journeyName + "Business {\n\n}");
    }

    private void writeToFile(File file, String content) throws IOException {
        if (!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
        }
    }

    public static void updateJourneyTypeEnum(String pathCore, String serviceName, String journeyType) throws IOException {
        File file = new File(pathCore+ "\\journey\\types\\JourneyType.java");
        Path filePath = file.toPath();
        String content = Files.readString(filePath, StandardCharsets.UTF_8);

        // Build enum constant code (lowercase alnum)
        String code = buildCode(serviceName);

        // Avoid duplicate insert if code already present
        Pattern existing = Pattern.compile("\\b" + Pattern.quote(journeyType) + "\\s*\\(\\s*\"" + Pattern.quote(code) + "\"\\s*\\)");
        if (existing.matcher(content).find()) {
            log.info("ℹ️ JourneyType already contains: {}(\"{}\")", journeyType, code);
            return;
        }

        // Find enum header and the semicolon that terminates the constants list
        Pattern header = Pattern.compile("\\benum\\s+JourneyType\\b[^{]*\\{", Pattern.DOTALL);
        Matcher m = header.matcher(content);
        if (!m.find()) {
            log.error("Cannot locate 'enum JourneyType {' in file: {}", filePath);
            throw new IllegalStateException("Cannot locate 'enum JourneyType {'.");
        }
        int bodyStart = m.end(); // position right after '{'
        int constantsSemi = content.indexOf(';', bodyStart);
        if (constantsSemi == -1) {
            log.error("Cannot find the semicolon ending the enum constants in file: {}", filePath);
            throw new IllegalStateException("Cannot find the semicolon ending the enum constants.");
        }

        // Determine if we need a comma before inserting
        int i = constantsSemi - 1;
        while (i > bodyStart && Character.isWhitespace(content.charAt(i))) i--;
        boolean needsComma = i >= bodyStart && content.charAt(i) != ',';

        String newEntry = journeyType + "(\"" + code + "\")";

        String updated =
                content.substring(0, constantsSemi) +
                        (needsComma ? "," : "") +
                        "\n    " + newEntry +
                        content.substring(constantsSemi);

        Files.writeString(filePath, updated, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("✅ Added JourneyType: {}", newEntry);
    }

    private static String buildCode(String serviceName) {
        String code = serviceName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (code.isEmpty()) throw new IllegalArgumentException("Invalid serviceName for code: " + serviceName);
        return code;
    }

    public static void generateController(
            String serviceName, String journeyType,
            String pathCore,
            String urlRestController,
            String urlEndPoint
    ) throws IOException {
        String journeyTypeLower = journeyType.substring(0, 1).toLowerCase() + journeyType.substring(1);
        String className = journeyType + "Controller";

        // Construire le dossier pour le controller dans pathCore
        String packagePath = pathCore + "/services/" + serviceName + "/controller";
        File dir = new File(packagePath);
        if (!dir.exists()) dir.mkdirs();

        // Contenu du controller
        String content = "package ae.gov.sdg.ext.rta.core.services." + serviceName + ".controller;\n\n" +
                "import ae.gov.digitaldubai.paperless.commons.exceptions.UserAuthenticationFailedException;\n" +
                "import ae.gov.digitaldubai.paperless.controller.BaseController;\n" +
                "import ae.gov.digitaldubai.paperless.security.authentication.LoginRequired;\n" +
                "import ae.gov.sdg.ext.rta.core.journey.model.JourneyRequest;\n" +
                "import lombok.RequiredArgsConstructor;\n" +
                "import lombok.extern.slf4j.Slf4j;\n" +
                "import org.springframework.http.HttpStatus;\n" +
                "import org.springframework.http.ResponseEntity;\n" +
                "import org.springframework.web.bind.annotation.*;\n\n" +
                "@Slf4j\n" +
                "@RestController\n" +
                "@RequestMapping(\"" + urlRestController + "\")\n" +
                "@RequiredArgsConstructor\n" +
                "public class " + className + " extends BaseController {\n\n" +
                "    @PostMapping(\"" + urlEndPoint + "\")\n" +
                "    @LoginRequired(userTypes = \"SOP1,SOP2,SOP3\")\n" +
                "    public ResponseEntity<String> " + journeyTypeLower + "Journey(@RequestBody JourneyRequest request) throws UserAuthenticationFailedException {\n" +
                "        log.info(\"" + journeyType + " Journey started\");\n\n" +
                "        return new ResponseEntity<>(screen(request), httpHeaders(request), HttpStatus.OK);\n" +
                "    }\n" +
                "}";

        // Créer le fichier Java
        File file = new File(dir, className + ".java");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

        log.info("Controller generated: {}", file.getAbsolutePath());
    }

    public static void generateJsonFiles(String pathResourceCore, String serviceName, List<ConfigDDAProjectDto.ScreenComponents> screenComponentsList) throws IOException {
        // 1️⃣ Sequence JSON
        createSequenceJson(pathResourceCore, serviceName, screenComponentsList);

        // 2️⃣ Route JSON
        createRouteJson(pathResourceCore, serviceName, screenComponentsList);
    }

    private static void createRouteJson(String pathResourceCore, String serviceName, List<ConfigDDAProjectDto.ScreenComponents> screenComponentsList) throws IOException {
        String routeDirPath = pathResourceCore + "/routes";
        File routeDir = new File(routeDirPath);
        if (!routeDir.exists()) routeDir.mkdirs();

        String routeFileName = "route-" + serviceName + ".json";
        File routeFile = new File(routeDir, routeFileName);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (ConfigDDAProjectDto.ScreenComponents screen : screenComponentsList) {
            String fullClassPath = convertToClassName(screen.getNameScreen(), serviceName);
            if (screen.isInitialScreen()) {
                sb.append("  \"initialScreen\": \"").append(fullClassPath).append("\",\n");
            }
            sb.append("  \"").append(screen.getNameScreen()).append("-template-screen").append("\": \"")
                    .append(fullClassPath).append("\",\n");
        }

        // enlever la dernière virgule
        if (screenComponentsList.size() > 0) {
            sb.setLength(sb.length() - 2);
            sb.append("\n");
        }
        sb.append("}");

        try (FileWriter writer = new FileWriter(routeFile)) {
            writer.write(sb.toString());
        }

        log.info("Route JSON created: {}", routeFile.getAbsolutePath());
    }

    private static void createSequenceJson(String pathResourceCore, String serviceName, List<ConfigDDAProjectDto.ScreenComponents> screenComponentsList) throws IOException {
        String sequenceDirPath = pathResourceCore + "/sequence";
        File sequenceDir = new File(sequenceDirPath);
        if (!sequenceDir.exists()) sequenceDir.mkdirs();

        File sequenceFile = new File(sequenceDir, "sequence-" + serviceName + ".json");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        for (ConfigDDAProjectDto.ScreenComponents screen : screenComponentsList) {
            if (screen.getScreenSequencesList() == null || screen.getScreenSequencesList().isEmpty()) continue;

            for (ConfigDDAProjectDto.ScreenSequence seq : screen.getScreenSequencesList()) {
                ObjectNode screenNode = mapper.createObjectNode();

                if (seq.getSequenceForksList() != null && !seq.getSequenceForksList().isEmpty()) {
                    // fork structure
                    if (seq.getNext() != null) {
                        screenNode.put("next", seq.getNext() != null ? seq.getNext() + "-template-screen" : "");
                    }
                    if (seq.getPrevious() != null) {
                        screenNode.put("previous", seq.getPrevious() != null ? seq.getPrevious() + "-template-screen" : "");
                    }
                    ObjectNode forkNode = mapper.createObjectNode();
                    forkNode.put("key", "currentAction"); // or pass dynamically if needed

                    ObjectNode valuesNode = mapper.createObjectNode();
                    for (ConfigDDAProjectDto.SequenceFork fork : seq.getSequenceForksList()) {
                        valuesNode.put(fork.getActionName(), fork.getScreenName() + "-template-screen");
                    }

                    forkNode.set("values", valuesNode);
                    screenNode.set("fork", forkNode);
                } else {
                    // simple next/previous
                    screenNode.put("next", seq.getNext() != null ? seq.getNext() + "-template-screen" : "");
                    screenNode.put("previous", seq.getPrevious() != null ? seq.getPrevious() + "-template-screen" : "");
                }

                root.set(screen.getNameScreen() + "-template-screen", screenNode);
            }
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(sequenceFile, root);

        log.info("Sequence JSON created: {}", sequenceFile.getAbsolutePath());
    }

    private static String convertToClassName(String nameScreen, String serviceName) {
        String[] parts = nameScreen.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return "ae.gov.sdg.ext.rta.core.services." + serviceName + ".service." + sb + "Service";
    }

    public static void generateConstantsFile(String pathCore, String serviceName, String journeyType, List<ConfigDDAProjectDto.ScreenComponents> screenComponentsList) throws IOException {
        // Dossier constants
        String constantsDirPath = pathCore + "/services/" + serviceName + "/constants";
        File constantsDir = new File(constantsDirPath);
        if (!constantsDir.exists()) constantsDir.mkdirs();

        String className = journeyType + "Constants";
        File constantsFile = new File(constantsDir, className + ".java");

        String packageName = "services." + serviceName + ".constants";

        StringBuilder sb = new StringBuilder();
        sb.append("package ae.gov.sdg.ext.rta.core.").append(packageName).append(";\n\n");
        sb.append("public class ").append(className).append(" {\n\n");

        for (ConfigDDAProjectDto.ScreenComponents screen : screenComponentsList) {
            // constante pour le nameScreen
            String constName = convertToConstant(screen.getNameScreen());
            sb.append("    public static final String ").append(constName)
                    .append(" = \"").append(screen.getNameScreen()).append("-template-screen").append("\";\n");
        }

        sb.append("\n}");

        try (FileWriter writer = new FileWriter(constantsFile)) {
            writer.write(sb.toString());
        }

        log.info("Constants file created: {}", constantsFile.getAbsolutePath());
    }

    public static void generateServices(String pathCore, String serviceName, String journeyType,
                                        List<ConfigDDAProjectDto.ScreenComponents> screenComponentsList) throws IOException {

        // dossier services/<serviceName>/service
        String serviceDirPath = pathCore + "/services/" + serviceName + "/service";
        File serviceDir = new File(serviceDirPath);
        if (!serviceDir.exists()) serviceDir.mkdirs();

        String constantsClassName = journeyType + "Constants";
        String constantsPackage = "ae.gov.sdg.ext.rta.core." + "services." + serviceName + ".constants";

        for (ConfigDDAProjectDto.ScreenComponents screen : screenComponentsList) {
            String serviceClassName = toCamelCase(screen.getNameScreen()) + "Service";
            File serviceFile = new File(serviceDir, serviceClassName + ".java");

            StringBuilder sb = new StringBuilder();
            // package
            sb.append("package ae.gov.sdg.ext.rta.core.services.").append(serviceName).append(".service;\n\n");

            // imports
            sb.append("import ae.gov.digitaldubai.paperless.journey.model.JourneyDTO;\n")
                    .append("import ae.gov.digitaldubai.paperless.journey.service.JourneyExecutionContext;\n")
                    .append("import ae.gov.sdg.ext.rta.core.journey.service.BaseMSJourneyService;\n")
                    .append("import ").append(constantsPackage).append(".").append(constantsClassName).append(";\n")
                    .append("import lombok.RequiredArgsConstructor;\n")
                    .append("import lombok.SneakyThrows;\n")
                    .append("import lombok.extern.slf4j.Slf4j;\n")
                    .append("import org.springframework.context.annotation.Scope;\n")
                    .append("import org.springframework.stereotype.Service;\n")
                    .append("import java.util.HashMap;\n")
                    .append("import java.util.Map;\n")
                    .append("import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;\n\n");

            // class declaration
            sb.append("@Slf4j\n@Service\n@Scope(SCOPE_PROTOTYPE)\n@RequiredArgsConstructor\n")
                    .append("public class ").append(serviceClassName).append(" extends BaseMSJourneyService {\n\n");

            // process method
            sb.append("    @SneakyThrows\n")
                    .append("    @Override\n")
                    .append("    public JourneyDTO process(JourneyExecutionContext context) {\n")
                    .append("        log.info(\"Start ").append(serviceClassName).append(" screen\");\n\n")
                    .append("        Map<String, Object> map = new HashMap<>();\n\n")
                    .append("        map.put(").append(constantsClassName).append(".")
                    .append(toConstant(screen.getNameScreen()))
                    .append(", context.getRequest().getLang());\n\n")
                    .append("        return new JourneyDTO(").append(constantsClassName).append(".")
                    .append(toConstant(screen.getNameScreen()))
                    .append(", map);\n")
                    .append("    }\n\n")
                    .append("}\n");

            try (FileWriter writer = new FileWriter(serviceFile)) {
                writer.write(sb.toString());
            }

            log.info("Service created: {}", serviceFile.getAbsolutePath());
        }
    }

    private static String toCamelCase(String name) {
        String[] parts = name.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return sb.toString();
    }

    private static String toConstant(String nameScreen) {
        // correspond à la constante générée dans Constants
        return nameScreen.toUpperCase().replace("-", "_") + "_TEMPLATE_SCREEN";
    }

    private static String convertToConstant(String nameScreen) {
        return nameScreen.toUpperCase().replace("-", "_") + "_TEMPLATE_SCREEN";
    }
}
