package com.example.demo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class VoucherCampaignForm {
    @NotBlank(message="Vui lòng nhập tên chương trình.") @Size(max=150,message="Tên chương trình tối đa 150 ký tự.") private String name;
    @NotNull(message="Vui lòng chọn loại giảm giá.") @Pattern(regexp="PERCENT|FIXED",message="Loại giảm giá không hợp lệ.") private String discountType = "FIXED";
    @NotNull(message="Vui lòng nhập mức giảm.") @DecimalMin(value="0.01",message="Mức giảm phải lớn hơn 0.") @Digits(integer=12,fraction=2,message="Mức giảm tối đa 12 chữ số và 2 số thập phân.") private BigDecimal discountValue;
    @NotNull(message="Vui lòng nhập đơn tối thiểu.") @DecimalMin(value="0",message="Đơn tối thiểu không được âm.") @Digits(integer=12,fraction=2,message="Đơn tối thiểu tối đa 12 chữ số và 2 số thập phân.") private BigDecimal minimumTotal = BigDecimal.ZERO;
    @DecimalMin(value="0.01",message="Giới hạn giảm phải lớn hơn 0.") @Digits(integer=12,fraction=2,message="Giới hạn giảm tối đa 12 chữ số và 2 số thập phân.") private BigDecimal maximumDiscount;
    @NotNull(message="Vui lòng chọn ngày hết hạn.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate expiryDate;
    public String getName(){return name;}
    public void setName(String v){name=v==null?null:v.strip();}
    public String getDiscountType(){return discountType;}
    public void setDiscountType(String v){discountType=v;}
    public BigDecimal getDiscountValue(){return discountValue;}
    public void setDiscountValue(BigDecimal v){discountValue=v;}
    public BigDecimal getMinimumTotal(){return minimumTotal;}
    public void setMinimumTotal(BigDecimal v){minimumTotal=v;}
    public BigDecimal getMaximumDiscount(){return maximumDiscount;}
    public void setMaximumDiscount(BigDecimal v){maximumDiscount=v;}
    public LocalDate getExpiryDate(){return expiryDate;}
    public void setExpiryDate(LocalDate v){expiryDate=v;}
}
