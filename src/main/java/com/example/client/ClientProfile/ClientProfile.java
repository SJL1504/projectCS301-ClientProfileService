package com.example.client.ClientProfile;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.Period;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name = "client_profiles",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "email"),
           @UniqueConstraint(columnNames = "phoneNumber")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientProfile {
    
    @Id
    @Column(nullable = false, updatable = false, unique = true)
    @NotNull(message = "Client ID cannot be null")
    private String clientId;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "First name must contain only alphabetic characters and spaces")
    private String firstName;
    
    @Column(nullable = false, length = 50)
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "Last name must contain only alphabetic characters and spaces")
    private String lastName;
    
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    @Column(nullable = false, length = 20)
    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(Male|Female|Non-binary|Prefer not to say)$", 
             message = "Gender must be one of: Male, Female, Non-binary, Prefer not to say")
    private String gender;
    
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email address is required")
    @Email(message = "Email must be in valid format")
    private String email;
    
    @Column(nullable = false, unique = true, length = 15)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?\\d{10,15}$", 
             message = "Phone number must be in valid format (e.g., +1234567890) with 10-15 digits")
    private String phoneNumber;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 100, message = "Address must be between 5 and 100 characters")
    private String address;
    
    @Column(nullable = false, length = 50)
    @NotBlank(message = "City is required")
    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    private String city;
    
    @Column(nullable = false, length = 50)
    @NotBlank(message = "State is required")
    @Size(min = 2, max = 50, message = "State must be between 2 and 50 characters")
    private String state;
    
    @Column(nullable = false, length = 50)
    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 50, message = "Country must be between 2 and 50 characters")
    private String country;
    
    @Column(nullable = false, length = 10)
    @NotBlank(message = "Postal code is required")
    @Size(min = 4, max = 10, message = "Postal code must be between 4 and 10 characters")
    private String postalCode;
    
    @Column(name = "agent_id")
    private String agentId;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    
    /**
     * JPA lifecycle callback to set default status before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = Status.PENDING;
        }
    }
    
    /**
     * Validates that the age is between 18 and 100 years
     * This method should be called during validation
     */
    @AssertTrue(message = "Age must be between 18 and 100 years")
    @JsonIgnore
    public boolean isAgeValid() {
        if (dateOfBirth == null) {
            return false;
        }
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return age >= 18 && age <= 100;
    }
    
    /**
     * Calculates the current age based on date of birth
     */
    @Transient
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
    
    /**
     * Status enum for client profile
     */
    public enum Status {
        VERIFIED,
        DELETED,
        PENDING
    }
}