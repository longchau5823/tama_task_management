package com.hutech.tama.repository;

import com.hutech.tama.entity.Role;
import com.hutech.tama.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
