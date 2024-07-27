package com.cortex.engine.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeExecutionTask {
  private String taskId;
  private SubmissionRequest submissionRequest;
}
