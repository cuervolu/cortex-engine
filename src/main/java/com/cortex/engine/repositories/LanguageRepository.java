package com.cortex.engine.repositories;

import com.cortex.engine.entities.Language;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LanguageRepository extends CrudRepository<Language, Long> {
  Optional<Language> findByName(String name);

  boolean existsByName(String name);
}
