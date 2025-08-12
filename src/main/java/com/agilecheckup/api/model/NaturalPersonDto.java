package com.agilecheckup.api.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaturalPersonDto {
  private String id;
  private String name;
  private String email;
  private String phone;
  private String documentNumber;
  private String personDocumentType;
  private String aliasName;
  private String gender;
  private String genderPronoun;
  private AddressDto address;
  private Date createdDate;

  private Date lastUpdatedDate;
}