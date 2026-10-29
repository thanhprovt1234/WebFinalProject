package com.example.demo.repository;

import com.example.demo.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByShopOrderOrderUserUsernameAndProductVariantProductSlugAndShopOrderStatus(
            String username, String productSlug, com.example.demo.entity.OrderStatus status);

    @org.springframework.data.jpa.repository.Query("""
            select distinct o from OrderItem i join i.shopOrder s join s.order o
            where o.user.username = :username
              and i.productVariant.product.slug = :productId
              and i.shopOrder.status = com.example.demo.entity.OrderStatus.DELIVERED
            order by o.id desc
            """)
    java.util.List<com.example.demo.entity.OrderEntity> findReviewableOrders(String username, String productId);

    interface Bestseller {
        Long getProductId();
        Long getSoldQuantity();
    }

    @org.springframework.data.jpa.repository.Query("""
            select p.id as productId, sum(i.quantity) as soldQuantity
            from OrderItem i join i.productVariant v join v.product p
            where i.shopOrder.status = com.example.demo.entity.OrderStatus.DELIVERED
              and i.shopOrder.order.paymentStatus = 'PAID'
              and p.enabled = true and p.category.enabled = true
              and p.shop.enabled = true and p.shop.status = com.example.demo.entity.ShopStatus.ACTIVE
            group by p.id
            order by sum(i.quantity) desc, p.id asc
            """)
    java.util.List<Bestseller> findBestsellers(org.springframework.data.domain.Pageable pageable);
}
