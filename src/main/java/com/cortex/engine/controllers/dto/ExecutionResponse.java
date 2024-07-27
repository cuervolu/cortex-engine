package com.cortex.engine.controllers.dto;

public record ExecutionResponse(
    String stdout,
    Integer statusId,
    String stderr
) {}