package com.hutech.tama.repository;

import com.hutech.tama.entity.User;
import com.hutech.tama.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long userId);

    List<User> findAllByOrderByCreatedAtDesc();

    @Query("""
            select count(distinct u)
            from User u
            join u.roles role
            where u.enabled = true and role.name = :roleName
            """)
    long countEnabledUsersByRole(@Param("roleName") RoleName roleName);
}
