package com.dda.ddaConfigurationApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ConfigDDAProjectDto {
    private String journeyType ;
    private String pathCore;
    private String pathResourceCore;
    private String pathResourceUI;
    private String urlRestController;
    private String urlEndPoint;
    private String screensTile;
    private String serviceName;
    private List<ScreenComponents> screenComponentsList;


    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ScreenComponents {
        private String nameScreen;
        private boolean initialScreen;
        private List<ScreenSequence> screenSequencesList;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ScreenSequence {
        private String next;
        private String previous;
        private List<SequenceFork> sequenceForksList;
    }
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class SequenceFork {
        private String actionName;
        private String screenName;
    }



    }
