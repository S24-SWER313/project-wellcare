package com.wellcare.wellcare.Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "messages")
public class Message {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        
        @ManyToOne(optional = false, fetch = FetchType.EAGER)
        @JoinColumn(name = "from_user_id")
        @JsonIgnoreProperties({ "name", "attachment", "degree", "specialty", "password", "email", "mobile", "bio",
                        "gender", "image", "role", "friends", "friendsNumber", "savedPost", "messages" })
        private User fromUser;

        
        @ManyToOne(optional = false, fetch = FetchType.EAGER)
        @JoinColumn(name = "to_user_id")
        @JsonIgnoreProperties({ "name", "attachment", "degree", "specialty", "password", "email", "mobile", "bio",
                        "gender", "image", "role", "friends", "friendsNumber", "savedPost", "messages" })
        private User toUser;


        @NotBlank(message = "Content cannot be blank")
        @Size(max = 1000, message = "Content length must be less than or equal to 1000 characters")
        @Column(name = "content", nullable = false, columnDefinition = "TEXT")
        private String content;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "message_attachment", joinColumns = @JoinColumn(name = "message_id"))
        @Column(name = "attachment_url")
        private List<String> attachment = new ArrayList<>();

        @Column(name = "status", columnDefinition = "TINYINT DEFAULT 0")
        private int status;

        @NotNull(message = "Time cannot be null")
        @Column(name = "time", nullable = false)
        private LocalDateTime time;

        public Message() {
        }

      
}
