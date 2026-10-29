package com.example.demo.repository;

import java.util.Optional;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select distinct u from User u join u.roles r where u.enabled=true and r.name=com.example.demo.entity.RoleName.ROLE_USER and (lower(u.username) like lower(concat('%',:q,'%')) or lower(u.email) like lower(concat('%',:q,'%')) or lower(u.fullName) like lower(concat('%',:q,'%'))) order by u.id")
    java.util.List<User> findVoucherRecipients(String q);

    Optional<User> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.username = :username")
    Optional<User> findForCartUpdate(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.username = :username")
    Optional<User> findForProfileUpdate(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.username = :username")
    Optional<User> findForActivityUpdate(@Param("username") String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths="roles")
    org.springframework.data.domain.Page<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String username,String fullName,org.springframework.data.domain.Pageable pageable);
    @Query("select count(u) from User u join u.roles r where r.name=:role and u.enabled=true")
    long countEnabledRole(@Param("role") com.example.demo.entity.RoleName role);
}
