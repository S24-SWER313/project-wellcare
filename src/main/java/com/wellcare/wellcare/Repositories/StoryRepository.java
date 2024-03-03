package com.wellcare.wellcare.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wellcare.wellcare.Models.Story;

public interface StoryRepository extends JpaRepository<Story, Long>{
    
}
