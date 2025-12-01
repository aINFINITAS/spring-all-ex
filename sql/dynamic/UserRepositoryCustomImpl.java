package com.example.dynamic;


import jakarta.persistence.EntityManager;

import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final EntityManager em;

    @Override
    public Page<UserResponseDto> search(UserSearchRequest req) {

        // =====================================================
        // 1. Base SQL (NO paging, NO order)
        // =====================================================
        StringBuilder baseSql = new StringBuilder("""
                    SELECT
                        u.id,
                        u.name,
                        u.email,
                        u.status,
                        u.age,
                        u.created_at
                    FROM users u
                    WHERE 1 = 1
                """);

        Map<String, Object> params = new HashMap<>();

        // =====================================================
        // 2. Dynamic WHERE
        // =====================================================
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            baseSql.append("""
                        AND (
                            LOWER(u.name)  LIKE LOWER(:keyword)
                            OR LOWER(u.email) LIKE LOWER(:keyword)
                        )
                    """);
            params.put("keyword", "%" + req.getKeyword().trim() + "%");
        }

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            baseSql.append(" AND u.status = :status ");
            params.put("status", req.getStatus().trim());
        }

        if (req.getAgeFrom() != null) {
            baseSql.append(" AND u.age >= :ageFrom ");
            params.put("ageFrom", req.getAgeFrom());
        }

        if (req.getAgeTo() != null) {
            baseSql.append(" AND u.age <= :ageTo ");
            params.put("ageTo", req.getAgeTo());
        }

        // =====================================================
        // 3. Safe ORDER BY (whitelist)
        // =====================================================
        String sortColumn = mapSortColumn(req.getSortBy());
        String sortDir = "ASC".equalsIgnoreCase(req.getSortDir()) ? "ASC" : "DESC";

        String orderByClause =
                " ORDER BY " + sortColumn + " " + sortDir;

        // =====================================================
        // 4. OFFSET / FETCH (Oracle 12c+)
        // =====================================================
        int page = req.getPage() != null && req.getPage() >= 0
                ? req.getPage() : 0;

        int size = req.getSize() != null && req.getSize() > 0
                ? req.getSize() : 10;

        int offset = page * size;

        String pagingClause = """
                    OFFSET :offset ROWS
                    FETCH NEXT :size ROWS ONLY
                """;

        String dataSql =
                baseSql + orderByClause + pagingClause;

        // =====================================================
        // 5. TypedQuery (Object[])
        // =====================================================
        TypedQuery<Object[]> dataQuery =
                (TypedQuery<Object[]>) em.createNativeQuery(dataSql, Object[].class);

        params.forEach(dataQuery::setParameter);
        dataQuery.setParameter("offset", offset);
        dataQuery.setParameter("size", size);

        List<Object[]> rows = dataQuery.getResultList();

        List<UserResponseDto> content = rows.stream()
                .map(this::mapRowToDto)
                .toList();

        // =====================================================
        // 6. COUNT query (NO ORDER BY)
        // =====================================================
        String countSql =
                "SELECT COUNT(1) FROM (" + baseSql + ") c";

        TypedQuery<Number> countQuery =
                (TypedQuery<Number>) em.createNativeQuery(countSql, Number.class);

        params.forEach(countQuery::setParameter);

        long total = countQuery.getSingleResult().longValue();

        // =====================================================
        // 7. Return Page
        // =====================================================
        return new PageImpl<>(
                content,
                PageRequest.of(page, size),
                total
        );
    }

    // =====================================================
    // 8. Safe sort mapping
    // =====================================================
    private String mapSortColumn(String sortBy) {
        if (sortBy == null) {
            return "u.created_at";
        }
        return switch (sortBy) {
            case "name" -> "u.name";
            case "age" -> "u.age";
            case "email" -> "u.email";
            case "createdAt" -> "u.created_at";
            default -> "u.created_at";
        };
    }

    // =====================================================
    // 9. Native row -> DTO
    // =====================================================
    private UserResponseDto mapRowToDto(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String name = (String) row[1];
        String email = (String) row[2];
        String status = (String) row[3];
        Integer age = row[4] != null
                ? ((Number) row[4]).intValue()
                : null;

        Timestamp ts = (Timestamp) row[5];

        return new UserResponseDto(
                id,
                name,
                email,
                status,
                age,
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
