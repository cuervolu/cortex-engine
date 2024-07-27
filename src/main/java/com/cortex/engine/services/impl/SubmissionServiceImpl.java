package com.cortex.engine.services.impl;

import com.cortex.engine.entities.Submission;
import com.cortex.engine.repositories.SubmissionRepository;
import com.cortex.engine.services.ISubmissionService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements ISubmissionService {

  private final SubmissionRepository submissionRepository;

  @Override
  public List<Submission> getAllSubmissions() {
    return (List<Submission>) submissionRepository.findAll();
  }

  @Override
  public Optional<Submission> getSubmissionById(Long id) {
    return submissionRepository.findById(id);
  }

  @Override
  @Transactional
  public Submission saveSubmission(Submission submission) {
    return submissionRepository.save(submission);
  }

  @Override
  @Transactional
  public void deleteSubmission(Long id) {
    submissionRepository.deleteById(id);
  }
}
