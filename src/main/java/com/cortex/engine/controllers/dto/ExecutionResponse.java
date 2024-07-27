package com.cortex.engine.controllers.dto;

import java.io.Serializable;

public record ExecutionResponse(
    String stdout,
    Integer statusId,
    String stderr
) implements Serializable {}