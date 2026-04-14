package com.example.client.ClientProfile.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientProfileUpdateDTO {
    
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "First name must contain only alphabetic characters and spaces")
    private String firstName;
    
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "Last name must contain only alphabetic characters and spaces")
    private String lastName;
    
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    @Pattern(regexp = "^(Male|Female|Non-binary|Prefer not to say)$", 
             message = "Gender must be one of: Male, Female, Non-binary, Prefer not to say")
    private String gender;
    
    @Email(message = "Email must be in valid format")
    private String email;
    
    @Pattern(regexp = "^\\+?\\d{10,15}$", 
             message = "Phone number must be in valid format (e.g., +1234567890) with 10-15 digits")
    private String phoneNumber;
    
    @Size(min = 5, max = 100, message = "Address must be between 5 and 100 characters")
    private String address;
    
    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    private String city;
    
    @Size(min = 2, max = 50, message = "State must be between 2 and 50 characters")
    private String state;
    
    @Size(min = 2, max = 50, message = "Country must be between 2 and 50 characters")
    private String country;
    
    @Size(min = 4, max = 10, message = "Postal code must be between 4 and 10 characters")
    private String postalCode;
}