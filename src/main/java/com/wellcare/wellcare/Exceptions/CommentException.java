package com.wellcare.wellcare.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CommentException extends Exception{
    
    public CommentException(String message){
            super(message);
       }


}
