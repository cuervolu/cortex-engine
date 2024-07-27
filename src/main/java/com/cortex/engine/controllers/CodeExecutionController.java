package com.cortex.engine.controllers;

import com.cortex.engine.controllers.dto.ExecutionResponse;
import com.cortex.engine.controllers.dto.SubmissionRequest;
import com.cortex.engine.services.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/execute")
@RequiredArgsConstructor
public class CodeExecutionController {

  private final CodeExecutionService codeExecutionService;

  @PostMapping
  public ResponseEntity<String> submitCode(@RequestBody SubmissionRequest request) {
    try {
      String taskId = codeExecutionService.submitCodeExecution(request);
      return ResponseEntity.ok(taskId);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @GetMapping("/{taskId}")
  public ResponseEntity<ExecutionResponse> getExecutionResult(@PathVariable String taskId) {
    try {
      ExecutionResponse result = codeExecutionService.getExecutionResult(taskId);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(new ExecutionResponse(null, 4, e.getMessage()));
    }
  }
}