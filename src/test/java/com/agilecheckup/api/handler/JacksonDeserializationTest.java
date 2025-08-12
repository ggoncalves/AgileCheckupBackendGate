package com.agilecheckup.api.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.agilecheckup.persistency.entity.person.Gender;
import com.agilecheckup.persistency.entity.person.GenderPronoun;
import com.agilecheckup.persistency.entity.person.NaturalPerson;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test to verify that Jackson properly handles empty strings for enum fields
 * when ACCEPT_EMPTY_STRING_AS_NULL_OBJECT is configured.
 */
public class JacksonDeserializationTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    // Configure Jackson to handle empty strings as null for enums (same as ApiGatewayHandler)
    objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
  }

  @Test
  void shouldDeserializeEmptyGenderFieldsAsNull() throws Exception {
    // Given - JSON with empty strings for gender fields (reproduces the bug scenario)
    String json = "{\n" + "  \"name\": \"John Doe\",\n" + "  \"email\": \"john@example.com\",\n" + "  \"gender\": \"\",\n" + "  \"genderPronoun\": \"\"\n" + "}";

    // When
    NaturalPerson person = objectMapper.readValue(json, NaturalPerson.class);

    // Then
    assertThat(person.getName()).isEqualTo("John Doe");
    assertThat(person.getEmail()).isEqualTo("john@example.com");
    assertThat(person.getGender()).isNull(); // Empty string should be converted to null
    assertThat(person.getGenderPronoun()).isNull(); // Empty string should be converted to null
  }

  @Test
  void shouldDeserializeNullGenderFieldsAsNull() throws Exception {
    // Given - JSON with explicit null values for gender fields
    String json = "{\n" + "  \"name\": \"Jane Doe\",\n" + "  \"email\": \"jane@example.com\",\n" + "  \"gender\": null,\n" + "  \"genderPronoun\": null\n" + "}";

    // When
    NaturalPerson person = objectMapper.readValue(json, NaturalPerson.class);

    // Then
    assertThat(person.getName()).isEqualTo("Jane Doe");
    assertThat(person.getEmail()).isEqualTo("jane@example.com");
    assertThat(person.getGender()).isNull();
    assertThat(person.getGenderPronoun()).isNull();
  }

  @Test
  void shouldDeserializeValidGenderFieldsCorrectly() throws Exception {
    // Given - JSON with valid gender values
    String json = "{\n" + "  \"name\": \"Alex Smith\",\n" + "  \"email\": \"alex@example.com\",\n" + "  \"gender\": \"MALE\",\n" + "  \"genderPronoun\": \"HE\"\n" + "}";

    // When
    NaturalPerson person = objectMapper.readValue(json, NaturalPerson.class);

    // Then
    assertThat(person.getName()).isEqualTo("Alex Smith");
    assertThat(person.getEmail()).isEqualTo("alex@example.com");
    assertThat(person.getGender()).isEqualTo(Gender.MALE);
    assertThat(person.getGenderPronoun()).isEqualTo(GenderPronoun.HE);
  }

  @Test
  void shouldDeserializeMissingGenderFieldsAsNull() throws Exception {
    // Given - JSON without gender fields
    String json = "{\n" + "  \"name\": \"Sam Wilson\",\n" + "  \"email\": \"sam@example.com\"\n" + "}";

    // When
    NaturalPerson person = objectMapper.readValue(json, NaturalPerson.class);

    // Then
    assertThat(person.getName()).isEqualTo("Sam Wilson");
    assertThat(person.getEmail()).isEqualTo("sam@example.com");
    assertThat(person.getGender()).isNull(); // Missing fields should be null
    assertThat(person.getGenderPronoun()).isNull(); // Missing fields should be null
  }
}