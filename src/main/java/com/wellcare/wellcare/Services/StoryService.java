package com.wellcare.wellcare.Services;

import java.util.List;

import com.wellcare.wellcare.Models.Story;

public interface StoryService {
    public Story createStory(Story story, Integer userId) throws Exception;

    public List<Story> findStoryByUserId(Integer userId) throws Exception;
}
