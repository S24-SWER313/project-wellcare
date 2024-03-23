package com.wellcare.wellcare.Exceptions;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ErrorDetails {
    
      private String message;
    private String details; 
    private LocalDateTime timestamp;


    public ErrorDetails(){}


    public ErrorDetails(String message, String details, LocalDateTime timestamp) {
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
    }


}
