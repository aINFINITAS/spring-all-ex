package com.example.dynamic;

import org.springframework.data.domain.Page;

public interface UserRepositoryCustom {

    Page<UserResponseDto> search(UserSearchRequest request);
}