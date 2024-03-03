package com.wellcare.wellcare.Models;

import java.util.*;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

    
enum Gender{
    FEMALE,
    MALE,
}

@Entity
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


    @ManyToMany
    @JoinTable(
        name = "user_followers",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "follower_id")
    )
    private Set<User> follower = new HashSet<>();

    @ManyToMany(mappedBy = "follower")
    private Set<User> following = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Story> stories = new ArrayList<>();

    @ManyToMany
    private List<Post> savedPost = new ArrayList<>();

    @ManyToMany
    private List<Post> sharedPost = new ArrayList<>();

    public User(){}
    
    public User(Long id, String username, String firstName, String lastName, String password, String email,
            String mobile, String bio, Gender gender, String image, Set<User> follower, Set<User> following,
            List<Story> stories, List<Post> savedPost, List<Post> sharedPost) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.email = email;
        this.mobile = mobile;
        this.bio = bio;
        this.gender = gender;
        this.image = image;
        this.follower = follower;
        this.following = following;
        this.stories = stories;
        this.savedPost = savedPost;
        this.sharedPost = sharedPost;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Set<User> getFollower() {
        return follower;
    }

    public void setFollower(Set<User> follower) {
        this.follower = follower;
    }

    public Set<User> getFollowing() {
        return following;
    }

    public void setFollowing(Set<User> following) {
        this.following = following;
    }

    public List<Story> getStories() {
        return stories;
    }

    public void setStories(List<Story> stories) {
        this.stories = stories;
    }

    public List<Post> getSavedPost() {
        return savedPost;
    }

    public void setSavedPost(List<Post> savedPost) {
        this.savedPost = savedPost;
    }

    public List<Post> getSharedPost() {
        return sharedPost;
    }

    public void setSharedPost(List<Post> sharedPost) {
        this.sharedPost = sharedPost;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", username=" + username + ", firstName=" + firstName + ", lastName=" + lastName
                + ", password=" + password + ", email=" + email + ", mobile=" + mobile + ", bio=" + bio + ", gender="
                + gender + ", image=" + image + ", follower=" + follower + ", following=" + following + ", stories="
                + stories + ", savedPost=" + savedPost + ", sharedPost=" + sharedPost + "]";
    }

    
}
