package com.wellcare.wellcare.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wellcare.wellcare.Models.ERole;
import com.wellcare.wellcare.Models.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Optional<Role> findByName(ERole name);
}
