package com.wellcare.wellcare.Models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
@Table(name = "likes")
public class Like {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "user_id")
  private User author;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "post_id")
  private Post post;

  public Like() {}

  public Like(Post post, User author) {
    this.post = post;
    this.author = author;
  }
}