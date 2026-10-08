package com.myboxydev.dev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Contrato de cadastro enviado pelo app (UserSignupRequestModel no Flutter).
 * Chaves em snake_case, espelhando a tabela user_profiles.
 */
public record UserSignupRequestDTO(
  @JsonProperty("full_name")
  @NotBlank(message = "Informe o nome completo")
  @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
  String fullName,

  @JsonProperty("alias")
  @NotBlank(message = "Informe o alias")
  @Size(min = 3, max = 31, message = "O alias deve ter entre 3 e 30 caracteres")
  String alias,

  @JsonProperty("cpf")
  @NotBlank(message = "Informe o CPF")
  String cpf,

  @JsonProperty("email")
  @NotBlank(message = "Informe o e-mail")
  @Email(message = "E-mail inválido")
  @Size(max = 255, message = "E-mail muito longo")
  String email,

  @JsonProperty("phone_number")
  @NotBlank(message = "Informe o telefone")
  @Pattern(regexp = "^[0-9+()\\-\\s]{10,20}$", message = "Telefone inválido")
  String phoneNumber,

  @JsonProperty("password")
  @NotBlank(message = "Informe a senha")
  @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
  String password,

  @JsonProperty("address")
  @NotNull(message = "Informe o endereço")
  @Valid
  UserSignupAddressRequestDTO address
) {
  public record UserSignupAddressRequestDTO(
    @JsonProperty("postal_code")
    @NotBlank(message = "Informe o CEP")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido")
    String postalCode,

    @JsonProperty("address_line1")
    @NotBlank(message = "Informe o logradouro")
    @Size(max = 255, message = "Logradouro muito longo")
    String addressLine1,

    @JsonProperty("number")
    @NotBlank(message = "Informe o número")
    @Size(max = 20, message = "Número muito longo")
    String number,

    @JsonProperty("address_line2")
    @Size(max = 100, message = "Complemento muito longo")
    String addressLine2,

    @JsonProperty("neighborhood")
    @NotBlank(message = "Informe o bairro")
    @Size(max = 100, message = "Bairro muito longo")
    String neighborhood,

    @JsonProperty("city")
    @NotBlank(message = "Informe a cidade")
    @Size(max = 100, message = "Cidade muito longa")
    String city,

    @JsonProperty("state")
    @NotBlank(message = "Informe a UF")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "UF inválida")
    String state,

    @JsonProperty("country")
    @Pattern(regexp = "^(?i)BR$", message = "No momento, apenas endereços no Brasil são aceitos")
    String country
  ) {}

  /** Evita vazar CPF e senha caso o record seja logado. */
  @Override
  public String toString() {
    return "UserSignupRequestDTO[fullName=" + fullName + ", alias=" + alias + ", email=" + email
            + ", cpf=***, password=***]";
  }
}
