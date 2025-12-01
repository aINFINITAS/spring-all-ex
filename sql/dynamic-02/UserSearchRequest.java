package com.example.dynamic;

import lombok.Data;

@Data
public class UserSearchRequest {

    // Điều hướng bằng switch
    private String searchType;
    // ALL | NAME | EMAIL | STATUS | AGE_RANGE

    private String keyword;
    private String status;
    private Integer ageFrom;
    private Integer ageTo;

    // Paging
    private Integer page = 0;
    private Integer size = 10;

    // Sorting
    private String sortBy = "createdAt";
    private String sortDir = "DESC";
}
