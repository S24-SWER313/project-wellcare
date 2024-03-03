package com.wellcare.wellcare.Models;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
    
    private @Id @GeneratedValue Long id;

    private String content; 
    private LocalDateTime createdAt;
    @Nullable
    private String location;
    @Nullable
    private String caption;
    @Enumerated(EnumType.STRING) 
    private PostType type; 
    @Nullable
    private String url; 

    @ManyToOne
    @JoinColumn(name = "author_id") 
    private User author;

    @ManyToMany(mappedBy = "likedPosts")
    private List<User> likedBy;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments;

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




}

