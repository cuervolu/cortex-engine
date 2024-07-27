package com.cortex.engine.services;

import com.cortex.engine.entities.Submission;
import java.util.List;
import java.util.Optional;

public interface ISubmissionService {
  List<Submission> getAllSubmissions();
  Optional<Submission> getSubmissionById(Long id);
  Submission saveSubmission(Submission submission);
  void deleteSubmission(Long id);
}