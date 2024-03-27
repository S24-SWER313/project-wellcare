package com.wellcare.wellcare.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
@Table(name = "role")
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Adjust the type of 'name' to match the enum type 'ERole'
  @NotNull(message = "Role name cannot be null")
  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private ERole name;

  public Role() {
  }

  public Role(ERole name) {
    this.name = name;
  }
}