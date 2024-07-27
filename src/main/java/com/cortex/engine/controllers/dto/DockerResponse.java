package com.cortex.engine.controllers.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DockerResponse {
  private String version;
  private String apiVersion;
  private String operatingSystem;
  private String architecture;
  private String kernelVersion;
  private String goVersion;
  private String experimentalBuild;
  private String serverVersion;
}
