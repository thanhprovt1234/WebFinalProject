package com.example.demo.repository;

import java.util.Optional;

import com.example.demo.entity.Role;
import com.example.demo.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Role r where r.name=:name")
    Optional<Role> lockRole(@org.springframework.data.repository.query.Param("name") RoleName name);
}
