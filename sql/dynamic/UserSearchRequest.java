package com.example.dynamic;

import lombok.Data;

@Data
public class UserSearchRequest {

    // Dynamic filters
    private String keyword;   // search name/email
    private String status;    // ACTIVE / INACTIVE
    private Integer ageFrom;
    private Integer ageTo;

    // Paging
    private Integer page = 0;   // zero-based
    private Integer size = 10;  // page size

    // Sorting
    private String sortBy = "createdAt";  // name, age, createdAt
    private String sortDir = "DESC";      // ASC / DESC
}