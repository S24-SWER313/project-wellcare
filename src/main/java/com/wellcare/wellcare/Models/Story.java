package com.wellcare.wellcare.Models;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String image;
    private String caption;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt; // You can set the duration of the story
   
   
    @ManyToOne
     @JsonIgnoreProperties({ "password", "name", "attachment", "degree", "specialty", "friends", "friendsNumber",
            "email", "mobile", "bio", "gender", "image", "role",
            "savedPost, stories, postsCount" })
    private User user;

    public Story(Long id, String image, String caption, LocalDateTime createdAt, LocalDateTime expiresAt, User user) {
        this.id = id;
        this.image = image;
        this.caption = caption;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.user = user;
    }

}
