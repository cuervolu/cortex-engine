package com.cortex.engine.entities;

import com.cortex.engine.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Submission extends BaseEntity {

  @Column(nullable = false, columnDefinition = "TEXT")
  private String code;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "language_id", nullable = false)
  private Language language;

  @Column(columnDefinition = "TEXT")
  private String stdin;

  @Column(name = "expected_output", columnDefinition = "TEXT")
  private String expectedOutput;

  @Column(name = "cpu_time_limit")
  private Float cpuTimeLimit;

  @Column(name = "cpu_extra_time")
  private Float cpuExtraTime;

  @Column(name = "command_line_arguments")
  private String commandLineArguments;

  @Column(name = "compiler_options")
  private String compilerOptions;
}
