package com.cortex.engine.services.impl;

import com.cortex.engine.entities.Language;
import com.cortex.engine.repositories.LanguageRepository;
import com.cortex.engine.services.ILanguageService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LanguageServiceImpl implements ILanguageService {

  private final LanguageRepository languageRepository;

  @Override
  public List<Language> getAllLanguages() {
    return (List<Language>) languageRepository.findAll();
  }

  @Override
  public Optional<Language> getLanguageByName(String name) {
    return languageRepository.findByName(name);
  }

  @Override
  @Transactional
  public Language saveLanguage(Language language) {
    return languageRepository.save(language);
  }
}
