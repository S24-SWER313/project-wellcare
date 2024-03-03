package com.wellcare.wellcare.Models;

import java.time.LocalDateTime;
import java.util.*;
import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    public enum PostType {
        PHOTO, TEXT, VIDEO
    }
    
    private @Id @GeneratedValue
    Long id;


    private LocalDateTime createdAt;

    @Nullable
    private String location;

    private String caption;

    private String content;

    private String url;

    @Enumerated(EnumType.STRING) 
    private PostType type; 


    @ManyToOne
    @JoinColumn(name = "author_id") 
    private User author;

    @ManyToMany
    @JoinTable(
    name = "post_likes",
    joinColumns = @JoinColumn(name = "post_id"),
    inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> likedBy = new HashSet<>();

    

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    public Post() {
        this.createdAt = LocalDateTime.now();
    }

    public Post(User author, String content) {
        this.author = author;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.type = PostType.TEXT;
    }
    public Post(User author,String location, String caption, PostType type, String url) {
        this.author = author;
        this.createdAt = LocalDateTime.now();
        this.location = location;
        this.caption = caption;
        this.type = type;
        this.url = url;
    }

    public Post(Long id, LocalDateTime createdAt, String location, String caption, PostType type, User author,
            Set<User> likedBy, List<Comment> comments) {
        this.id = id;
        this.createdAt = createdAt;
        this.location = location;
        this.caption = caption;
        this.type = type;
        this.author = author;
        this.likedBy = likedBy;
        this.comments = comments;
    }

   




}

