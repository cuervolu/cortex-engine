package com.cortex.engine.services;

import com.cortex.engine.entities.Language;
import java.util.List;
import java.util.Optional;

public interface ILanguageService {
  List<Language> getAllLanguages();

  Optional<Language> getLanguageByName(String name);

  Language saveLanguage(Language language);
}
