package com.wellcare.wellcare.Models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Data
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String attachment;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({ "name", "degree", "specialty", "password", "email", "mobile", "bio", "gender",
            "role",
            "attachment", "friends", "friendsNumber", "savedPost" })
    private User user;

    private long noOfLikes;

    @Size(max = 500, message = "Content length must be less than or equal to 500 characters")
    private String content;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "comment_likes", joinColumns = @JoinColumn(name = "comment_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIgnoreProperties({ "name", "degree", "specialty", "password", "email", "mobile", "bio", "gender", "image",
            "role",
            "attachment", "friends", "friendsNumber", "savedPost" })
    private Set<User> commentLikes = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @AssertTrue(message = "Either attachment or content must be provided")
    public boolean isEitherAttachmentOrContentValid() {
        if (content == null || content.isBlank()) {
            return attachment == null || attachment.isBlank();
        } else {
            return true;
        }
    }

    public Comment() {
    }

    public Comment(User user, String content) {
        this.user = user;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.noOfLikes = 0;
    }

    public Comment(User user, String content, String attachment) {
        this.user = user;
        this.content = content;
        this.attachment = attachment;
        this.createdAt = LocalDateTime.now();
        this.noOfLikes = 0;
    }

}
