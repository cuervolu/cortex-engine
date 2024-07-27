package com.cortex.engine.workers;

import com.cortex.engine.controllers.dto.CodeExecutionTask;
import com.cortex.engine.services.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionWorker {

  private final CodeExecutionService codeExecutionService;

  @RabbitListener(queues = "codeExecution")
  public void processCodeExecution(CodeExecutionTask task) {
    log.info("Received code execution task: {}", task.getTaskId());
    codeExecutionService.processCodeExecution(task);
  }
}
