package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name="store_managers",uniqueConstraints=@UniqueConstraint(name="uk_store_managers_user_store",columnNames={"user_id","store_id"}))
public class StoreManager extends BaseEntity {
    @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false)
    private User user;
    @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="store_id",nullable=false)
    private Store store;
    public User getUser(){return user;}
    public void setUser(User user){this.user=user;}
    public Store getStore(){return store;}
    public void setStore(Store store){this.store=store;}
}
