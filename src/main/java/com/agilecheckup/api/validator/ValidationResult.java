package com.agilecheckup.api.validator;

import lombok.Getter;

import java.util.List;

public class ValidationResult {
  @Getter
  private final boolean valid;
  private final List<String> errors;

  public ValidationResult(boolean valid, List<String> errors) {
    this.valid = valid;
    this.errors = errors;
  }

  public String getErrorMessage() {
    return String.join(", ", errors);
  }
}