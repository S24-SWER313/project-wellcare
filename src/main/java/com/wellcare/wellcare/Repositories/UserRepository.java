package com.wellcare.wellcare.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;
import com.wellcare.wellcare.Models.User;

public interface UserRepository extends JpaRepository<User, Long> {

    public Optional<User> findByEmail(String email);

    public Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.id IN :user") 
    public List<User> findAllByUserId(@Param("user") List<Long> userId);
    
    @Query("SELECT DISTINCT u FROM User u WHERE u.username LIKE %:query% OR u.email LIKE %:query%")
    public List<User> findBySearch(@Param("query") String query);

    
}
