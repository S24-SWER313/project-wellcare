package com.wellcare.wellcare.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wellcare.wellcare.Models.Story;

public interface StoryRepository extends JpaRepository<Story, Integer> {
    @Query("select * from story where user.id= :userId")
    public List<Story> findStoryByUserId(@Param("userId") Integer userId);

}
