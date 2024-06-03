package com.wellcare.wellcare.Models;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "relationship")
public class Relationship {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NotNull(message = "User one must not be null")
        @ManyToOne(optional = false, fetch = FetchType.EAGER)
        @JoinColumn(name = "user_one_id", referencedColumnName = "id")
        @JsonIgnoreProperties({ "name", "attachment", "degree", "specialty", "password", "email", "mobile", "bio",
                        "gender", "image", "role", "friends", "friendsNumber", "savedPost", "messages" })
        private User userOne;

        @NotNull(message = "User two must not be null")
        @ManyToOne(optional = false, fetch = FetchType.EAGER)
        @JoinColumn(name = "user_two_id", referencedColumnName = "id")
        @JsonIgnoreProperties({ "name", "attachment", "degree", "specialty", "password", "email", "mobile", "bio",
                        "gender", "image", "role", "friends", "friendsNumber", "savedPost", "messages" })
        private User userTwo;

        @Column(name = "status", columnDefinition = "TINYINT DEFAULT 0", nullable = false)
        private int status;

        @NotNull(message = "Action user must not be null")
        @ManyToOne(optional = false, fetch = FetchType.EAGER)
        @JoinColumn(name = "action_user_id", referencedColumnName = "id")
        @JsonIgnoreProperties({ "name", "attachment", "degree", "specialty", "password", "email", "mobile", "bio",
                        "gender", "image", "role", "friends", "friendsNumber", "savedPost", "messages" })
        private User actionUser;

        @NotNull(message = "Time must not be null")
        @Column(name = "time", nullable = false)
        private LocalDateTime time;

        public Relationship() {
        }
}
