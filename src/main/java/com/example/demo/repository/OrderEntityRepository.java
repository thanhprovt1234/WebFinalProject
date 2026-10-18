package com.example.demo.repository;

import com.example.demo.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEntityRepository extends JpaRepository<OrderEntity, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from OrderEntity o where o.id=:id and o.user.username=:username")
    java.util.Optional<OrderEntity> lockByIdAndUserUsername(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("username") String username);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from OrderEntity o where o.id=:id")
    java.util.Optional<OrderEntity> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    java.util.Optional<OrderEntity> findByUserIdAndCheckoutToken(Long userId, String checkoutToken);
    java.util.Optional<OrderEntity> findByIdAndUserUsername(Long id, String username);
    java.util.List<OrderEntity> findByUserUsernameOrderByIdDesc(String username);
}
