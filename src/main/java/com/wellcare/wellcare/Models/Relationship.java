package com.wellcare.wellcare.Models;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "relationship")
public class Relationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_one_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"name", "attachment","degree","specialty","password", "email", "mobile", "bio", "gender", "image", "role",
    "friends","friendsNumber", "savedPost", "messages"})
    private User userOne;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_two_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"name", "attachment","degree","specialty","password", "email", "mobile", "bio", "gender", "image", "role",
    "friends","friendsNumber", "savedPost", "messages"})
    private User userTwo;

    @Column(name = "status", columnDefinition = "TINYINT DEFAULT 0", nullable = false)
    private int status;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "action_user_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"name", "attachment","degree","specialty","password", "email", "mobile", "bio", "gender", "image", "role",
    "friends","friendsNumber", "savedPost", "messages"})
    private User actionUser;

    @Column(name = "time", nullable = false)
    private LocalDateTime time;

    @JsonIgnore
    @OneToMany(mappedBy = "relationship", targetEntity = Message.class, cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Message> messageList;

    public Relationship() {
    }
}
