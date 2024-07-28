package com.cortex.engine.workers;

import com.cortex.engine.controllers.dto.CodeExecutionTask;
import com.cortex.engine.services.impl.CodeExecutionServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionWorker {

  private final CodeExecutionServiceImpl codeExecutionServiceImpl;

  @RabbitListener(queues = "codeExecution")
  public void processCodeExecution(CodeExecutionTask task) {
    log.info("Received code execution task: {}", task.getTaskId());
    codeExecutionServiceImpl.processCodeExecution(task);
  }
}
