package com.agilecheckup.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDto {
    private String id;
    private String street;
    private String city;
    private String state;
    private String country;
    private String zipcode;
}