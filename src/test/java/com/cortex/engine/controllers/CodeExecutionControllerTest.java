package com.cortex.engine.controllers;

import com.cortex.engine.controllers.dto.ExecutionResponse;
import com.cortex.engine.controllers.dto.SubmissionRequest;
import com.cortex.engine.controllers.dto.SubmissionResponse;
import com.cortex.engine.exceptions.CodeExecutionException;
import com.cortex.engine.services.impl.CodeExecutionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CodeExecutionControllerTest {

  @Mock private CodeExecutionServiceImpl codeExecutionService;

  @InjectMocks private CodeExecutionController codeExecutionController;

  private AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @Test
  void submitCode_SuccessfulSubmission_ReturnsOkResponse() {
    // Arrange
    SubmissionRequest request =
        new SubmissionRequest(
            "print('Hello, World!')", "python", null, null, null, null, null, null);
    String taskId = "task-123";
    when(codeExecutionService.submitCodeExecution(request)).thenReturn(taskId);

    // Act
    ResponseEntity<SubmissionResponse> response = codeExecutionController.submitCode(request);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(taskId, response.getBody().taskId());
    assertEquals("Code submission successful", response.getBody().message());

    verify(codeExecutionService, times(1)).submitCodeExecution(request);
  }

  @Test
  void submitCode_Base64EncodedCode_SuccessfulSubmission() {
    // Arrange
    String originalCode = "print('Hello, World!')";
    String base64EncodedCode = Base64.getEncoder().encodeToString(originalCode.getBytes());
    SubmissionRequest request =
        new SubmissionRequest(
            base64EncodedCode, "python", null, null, null, null, null, true);
    String taskId = "task-123";
    when(codeExecutionService.submitCodeExecution(any(SubmissionRequest.class))).thenReturn(taskId);

    // Act
    ResponseEntity<SubmissionResponse> response = codeExecutionController.submitCode(request);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(taskId, response.getBody().taskId());
    assertEquals("Code submission successful", response.getBody().message());

    verify(codeExecutionService, times(1)).submitCodeExecution(argThat(submissionRequest -> {
      String decodedCode = new String(Base64.getDecoder().decode(submissionRequest.code()));
      return decodedCode.equals(originalCode) && submissionRequest.encodeOutputToBase64();
    }));
  }

  @Test
  void getExecutionResult_SuccessfulExecution_ReturnsOkResponse() throws CodeExecutionException {
    // Arrange
    String taskId = "task-123";
    ExecutionResponse executionResponse = new ExecutionResponse("Hello, World!", 0, null);
    when(codeExecutionService.getExecutionResult(taskId)).thenReturn(executionResponse);

    // Act
    ResponseEntity<ExecutionResponse> response = codeExecutionController.getExecutionResult(taskId);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(executionResponse, response.getBody());

    verify(codeExecutionService, times(1)).getExecutionResult(taskId);
  }

  @AfterEach
  void tearDown() throws Exception {
    closeable.close();
  }
}