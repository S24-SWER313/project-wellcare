package com.wellcare.wellcare.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    // Modify role field to be a String
    @NotBlank
    private String role;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

    // New fields for first name and last name
    private String name;
  
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Getter and setter methods for first name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


  
}
