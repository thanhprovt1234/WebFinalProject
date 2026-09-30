package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class ReviewRequest {
    @NotNull(message = "Vui lòng chọn số sao.")
    @Min(value = 1, message = "Số sao phải từ 1 đến 5.")
    @Max(value = 5, message = "Số sao phải từ 1 đến 5.")
    private Integer rating;

    @NotBlank(message = "Vui lòng nhập nhận xét.")
    @Size(max = 2000, message = "Nhận xét không được quá 2.000 ký tự.")
    private String comment;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment == null ? null : comment.strip(); }
}
