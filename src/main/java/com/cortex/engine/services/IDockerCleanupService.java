package com.cortex.engine.services;

public interface IDockerCleanupService {
  void cleanupStoppedContainers();
}
