package com.trolleycompare.backend.repository;

import com.trolleycompare.backend.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.trolleycompare.backend.model.AppUser.Role;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRoleAndEnabledTrue(Role role);
}
