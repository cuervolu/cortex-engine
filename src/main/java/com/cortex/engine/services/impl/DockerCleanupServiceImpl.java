package com.cortex.engine.services.impl;

import com.cortex.engine.services.IDockerCleanupService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerCleanupServiceImpl implements IDockerCleanupService {

  private final DockerClient dockerClient;

  @Value("${docker.cleanup.container.max-age-hours:24}")
  private int maxContainerAgeHours;

  @Override
  @Scheduled(fixedDelayString = "${docker.cleanup.interval-ms:3600000}")
  public void cleanupStoppedContainers() {
    log.info("Starting cleanup of stopped containers");
    try {
      List<Container> stoppedContainers = listStoppedContainers();
      for (Container container : stoppedContainers) {
        if (shouldRemoveContainer(container)) {
          removeContainer(container);
        }
      }
      log.info("Finished cleanup of stopped containers");
    } catch (Exception e) {
      log.error("Error during container cleanup", e);
    }
  }

  private List<Container> listStoppedContainers() {
    return dockerClient
        .listContainersCmd()
        .withShowAll(true)
        .withStatusFilter(List.of("exited"))
        .exec();
  }

  private boolean shouldRemoveContainer(Container container) {
    long containerAge = System.currentTimeMillis() / 1000 - container.getCreated();
    long maxAgeSeconds = maxContainerAgeHours * 3600L;
    return containerAge > maxAgeSeconds;
  }

  private void removeContainer(Container container) {
    try {
      log.info("Removing container: {}", container.getId());
      dockerClient.removeContainerCmd(container.getId()).exec();
      log.info("Successfully removed container: {}", container.getId());
    } catch (Exception e) {
      log.error("Failed to remove container: {}", container.getId(), e);
    }
  }
}
