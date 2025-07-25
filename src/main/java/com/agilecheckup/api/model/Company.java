package com.agilecheckup.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Company {

  private String id;

  private String documentNumber;

  private String name;

  private String legalName;

  private String email;

  private String description;

  private String tenantId;

  private String size;

  private String industry;

  private String website;

  private String phone;

  private NaturalPersonDto contactPerson;

  private AddressDto address;
}