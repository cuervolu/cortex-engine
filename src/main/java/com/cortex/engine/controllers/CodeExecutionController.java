package com.cortex.engine.controllers;

import com.cortex.engine.controllers.dto.ExecutionResponse;
import com.cortex.engine.controllers.dto.SubmissionRequest;
import com.cortex.engine.services.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/execute")
@RequiredArgsConstructor
public class CodeExecutionController {

  private final CodeExecutionService codeExecutionService;

  @PostMapping
  public ResponseEntity<ExecutionResponse> executeCode(@RequestBody SubmissionRequest request) {
    try {
      ExecutionResponse response = codeExecutionService.executeCode(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(new ExecutionResponse(null, 1, e.getMessage()));
    }
  }
}
