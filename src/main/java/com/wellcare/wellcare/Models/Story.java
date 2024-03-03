package com.wellcare.wellcare.Models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "story")
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
}