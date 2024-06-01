package com.wellcare.wellcare.Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @JsonProperty("location")
    @Size(max = 255)
    private String location;

    @NotBlank
    @Size(max = 255)
    private String content;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_attachment", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "attachment_url")
    private List<String> attachment = new ArrayList<>();

    @NotNull
    @Min(0)
    private Integer noOfLikes = 0;

    @NotNull
    @Min(0)
    private Integer noOfComments = 0;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnoreProperties({ "name", "degree", "specialty", "password", "email", "mobile", "bio", "gender",
            "role",
            "attachment", "friends", "friendsNumber", "savedPost" })
    private User user;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "post_likes", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIgnoreProperties({ "name", "degree", "specialty", "password", "email", "mobile", "bio", "gender", "image",
            "role",
            "attachment", "friends", "friendsNumber", "savedPost" })
    private Set<User> likes = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true)
    @JsonIgnoreProperties({ "user", "commentLikes", "createdAt" })
    private List<Comment> comments = new ArrayList<>();

    boolean isSaved;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "post_saved", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIgnoreProperties({ "name", "degree", "specialty", "password", "email", "mobile", "bio", "gender", "image",
            "role",
            "attachment", "friends", "friendsNumber", "savedPost", "postsCount" })
    private Set<User> savedBy = new HashSet<>();

    public Post() {
        this.createdAt = LocalDateTime.now();
        this.noOfLikes = 0;
    }

    public Post(String content) {
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.noOfLikes = 0;
        this.noOfComments = 0;
    }

    public Post(String location, List<String> attachment) {
        this.createdAt = LocalDateTime.now();
        this.location = location;
        this.attachment = new ArrayList<String>(attachment);
        this.noOfLikes = 0;
        this.noOfComments = 0;
    }

}
