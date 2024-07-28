package com.cortex.engine.services;

import com.cortex.engine.controllers.dto.CodeExecutionTask;
import com.cortex.engine.controllers.dto.ExecutionResponse;
import com.cortex.engine.controllers.dto.SubmissionRequest;
import com.cortex.engine.exceptions.CodeExecutionException;
import com.cortex.engine.exceptions.UnsupportedLanguageException;

public interface ICodeExecutionService {
  String submitCodeExecution(SubmissionRequest request) throws UnsupportedLanguageException;

  ExecutionResponse getExecutionResult(String taskId) throws CodeExecutionException;

  void processCodeExecution(CodeExecutionTask task);
}
