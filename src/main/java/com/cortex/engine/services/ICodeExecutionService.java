package com.cortex.engine.services;

import com.cortex.engine.controllers.dto.CodeExecutionTask;
import com.cortex.engine.controllers.dto.ExecutionResponse;
import com.cortex.engine.controllers.dto.SubmissionRequest;
import com.cortex.engine.exceptions.CodeExecutionException;
import com.cortex.engine.exceptions.UnsupportedLanguageException;

public interface ICodeExecutionService {

  /**
   * Submits a code execution task to the queue.
   *
   * @param request The submission request containing code and execution parameters
   * @return A unique task ID for retrieving the execution result
   * @throws UnsupportedLanguageException if the specified language is not supported
   */
  String submitCodeExecution(SubmissionRequest request) throws UnsupportedLanguageException;

  /**
   * Retrieves the execution result for a given task ID.
   *
   * @param taskId The unique identifier of the execution task
   * @return The execution response containing stdout, stderr, and status
   * @throws CodeExecutionException if the execution result is not available
   */
  ExecutionResponse getExecutionResult(String taskId) throws CodeExecutionException;

  /**
   * Processes a code execution task.
   *
   * @param task The code execution task to process
   */
  void processCodeExecution(CodeExecutionTask task);
}
