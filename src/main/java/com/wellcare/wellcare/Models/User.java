package com.wellcare.wellcare.Models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Data;

@Entity
@Data
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull(groups = { Create.class })
    @Size(min = 6, message = "Username should have at least 6 characters")
    private String username;

    @Size(min = 2, message = "Name should have at least 2 characters")
    private String name;

    @NotNull(groups = { Create.class })
    @Size(min = 8, message = "Password should have at least 8 characters")
    private String password;

    @Email(message = "Please enter a valid email address")
    private String email;

    private String mobile;
    private String bio;
    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String image;

    private String attachment;
    private String degree;
    private String specialty;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_followers", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "follower_id"))
    private Set<User> follower = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "follower")
    private Set<User> following = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_saved_posts", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "post_id"))
    private List<Post> savedPost = new ArrayList<>();

    public User() {
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(String username, String name, String password, String email, Role role) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    public User(
            String username,
            String name,
            String password,
            String email, String attachment, String degree,
            String specialty, Role role) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.email = email;
        this.attachment = attachment;
        this.degree = degree;
        this.specialty = specialty;
        this.role = role;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", username=" + username + ", name=" + name
                + ", email=" + email + ", mobile=" + mobile + ", bio=" + bio + ", gender="
                + gender + ", image=" + image + ", follower=" + follower + ", following=" + following + ", savedPost="
                + savedPost + "]";
    }

    public interface Create extends Default {
    }
}