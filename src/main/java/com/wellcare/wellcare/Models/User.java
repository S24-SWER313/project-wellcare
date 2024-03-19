package com.wellcare.wellcare.Models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import lombok.Data;

enum Gender {
    FEMALE,
    MALE,
} 

@Entity
@Data
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String password;
    private String email;
    private String mobile;
    private String bio;
    private Gender gender;
    private String image;
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_followers", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "follower_id"))
    private Set<User> follower = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "follower")
    private Set<User> following = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Post> savedPost = new ArrayList<>();
    

    public User() {
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(Long id, String username, String firstName, String lastName, String password, String email, Role role) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", username=" + username + ", firstName=" + firstName + ", lastName=" + lastName
                + ", email=" + email + ", mobile=" + mobile + ", bio=" + bio + ", gender="
                + gender + ", image=" + image + ", follower=" + follower + ", following=" + following + ", savedPost="
                + savedPost + "]";
    }

}