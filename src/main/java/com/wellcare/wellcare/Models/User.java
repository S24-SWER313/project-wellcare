package com.wellcare.wellcare.Models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
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
    @JsonIgnore
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

    @Enumerated(EnumType.STRING)
    private ERole role;

    @NotNull
    @Min(0)
    private Integer friendsNumber = 0;

    @NotNull
    @Min(0)
    private Integer postsCount = 0;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_friends", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "friend_id"))
    @JsonIgnoreProperties({ "password", "name", "attachment", "degree", "specialty", "friends", "friendsNumber",
            "email", "mobile", "bio", "gender", "image", "role",
            "savedPost" })
    private List<User> friends = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Story> stories = new ArrayList<>();

    public void addFriend(User friend) {
        if (!this.friends.contains(friend)) {
            this.friends.add(friend);
            friend.getFriends().add(this);
            friendsNumber++;

        }
    }

    public void removeFriend(User friend) {
        this.friends.remove(friend);
        friend.getFriends().remove(this);
        friendsNumber--;
    }

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_saved_posts", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "post_id"))
    @JsonIgnore
    private List<Post> savedPost = new ArrayList<>();

    public User() {
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(String username, String name, String password, String email, ERole role) {
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
            String specialty, ERole role) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.email = email;
        this.attachment = attachment;
        this.degree = degree;
        this.specialty = specialty;
        this.role = role;
    }

    public void incrementFriendsNumber() {
        this.friendsNumber++;
    }

    public void decrementFriendsNumber() {
        this.friendsNumber--;
        if (this.friendsNumber < 0) {
            this.friendsNumber = 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        if (ERole.PATIENT.equals(role)) {
            return "User [id=" + id + ", username=" + username + ", name=" + name
                    + ", email=" + email + ", mobile=" + mobile + ", bio=" + bio + ", gender="
                    + gender + ", image=" + image + ", role=" + role + ", savedPost="
                    + savedPost.size() + "]";
        } else {
            return "User [id=" + id + ", username=" + username + ", name=" + name
                    + ", email=" + email + ", mobile=" + mobile + ", bio=" + bio + ", gender="
                    + gender + ", image=" + image + ", attachment=" + attachment + ", degree=" + degree + ", specialty="
                    + specialty
                    + ", role=" + role + ", savedPost="
                    + savedPost.size() + "]";
        }
    }

    public interface Create extends Default {
    }

}
