package com.wellcare.wellcare.Models;

import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "comment")
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String content;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "comment_likes",
            joinColumns = @JoinColumn(name = "comment_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> commentLikes = new HashSet<>();


    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    
    public Comment(){}

    public Comment(Long id, User user, String content, Set<User> commentLikes, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.content = content;
        this.commentLikes = commentLikes;
        this.createdAt = createdAt;
    }
    

}
