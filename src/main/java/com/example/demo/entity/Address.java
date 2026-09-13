package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "addresses")
public class Address extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 150)
    @Nationalized
    @Column(name = "receiver_name", nullable = false, length = 150)
    private String receiverName;

    @NotBlank
    @Size(max = 20)
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @NotBlank
    @Size(max = 500)
    @Nationalized
    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @NotBlank
    @Size(max = 100)
    @Nationalized
    @Column(name = "ward", nullable = false, length = 100)
    private String ward;

    @NotBlank
    @Size(max = 100)
    @Nationalized
    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @NotBlank
    @Size(max = 100)
    @Nationalized
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress = false;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isDefault() {
        return defaultAddress;
    }

    public void setDefault(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }
}
