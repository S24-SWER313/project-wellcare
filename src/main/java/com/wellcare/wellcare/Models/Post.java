package com.wellcare.wellcare.Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
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

    @Id @GeneratedValue 
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private LocalDateTime createdAt;

    @JsonProperty("location")
    private String location;

    private String content;

    private List<String> attachment;

    private Integer noOfLikes = 0 ;

    private Integer noOfComments = 0;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"firstName", "lastName","password", "email", "mobile", "bio", "gender", "image", "role", "follower", "following", "savedPost"})
    private User user;

    

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "post_likes", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIgnoreProperties({"firstName", "lastName","password", "email", "mobile", "bio", "gender", "image", "role", "follower", "following", "savedPost"})
    private Set<User> likes = new HashSet<>();

    @OneToMany(mappedBy = "post", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("post")
    private List<Comment> comments = new ArrayList<>();

    public Post() {
        this.createdAt = LocalDateTime.now();
        this.noOfLikes = 0 ;
    }

    public Post(String content) {
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.noOfLikes = 0;
        this.noOfComments = 0 ;
    }

    public Post(String location, List<String> attachment) {
        this.createdAt = LocalDateTime.now();
        this.location = location;
        this.attachment = new ArrayList<String>(attachment);
        this.noOfLikes = 0;
        this.noOfComments = 0;
    }

}
