package com.agilecheckup.api.validator;

import java.util.ArrayList;
import java.util.List;

import com.agilecheckup.api.model.Company;

public class CompanyValidator {

  public static ValidationResult validate(Company company) {
    List<String> errors = new ArrayList<>();

    if (company.getDocumentNumber() == null || company.getDocumentNumber().trim().isEmpty()) {
      errors.add("Document number is required");
    }

    if (company.getName() == null || company.getName().trim().isEmpty()) {
      errors.add("Company name is required");
    }

    if (company.getEmail() == null || company.getEmail().trim().isEmpty()) {
      errors.add("Email is required");
    }
    else if (!isValidEmail(company.getEmail())) {
      errors.add("Invalid email format");
    }

    if (company.getDescription() == null || company.getDescription().trim().isEmpty()) {
      errors.add("Description is required");
    }

    if (company.getTenantId() == null || company.getTenantId().trim().isEmpty()) {
      errors.add("Tenant ID is required");
    }

    if (company.getSize() == null) {
      errors.add("Company size is required");
    }

    if (company.getIndustry() == null) {
      errors.add("Industry is required");
    }

    return new ValidationResult(errors.isEmpty(), errors);
  }

  private static boolean isValidEmail(String email) {
    return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
  }
}