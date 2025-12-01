package com.example.dynamic;


import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private String status;
    private Integer age;
    private LocalDateTime createdAt;
}