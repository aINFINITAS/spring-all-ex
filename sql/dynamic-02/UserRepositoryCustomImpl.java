package com.example.dynamic;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements  UserRepositoryCustom {

    private final EntityManager em;
    @Override
    public Page<UserResponseDto> search(UserSearchRequest req) {

        StringBuilder sql = new StringBuilder("""
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
        // ✅ 1. SWITCH-CASE Dynamic Search
        // =====================================================
        String type = req.getSearchType() != null
                ? req.getSearchType().toUpperCase()
                : "ALL";

        switch (type) {

            case "ALL" -> {
                if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                    sql.append("""
                                AND (
                                    LOWER(u.name)  LIKE LOWER(:kw)
                                    OR LOWER(u.email) LIKE LOWER(:kw)
                                    OR LOWER(u.status) LIKE LOWER(:kw)
                                )
                            """);
                    params.put("kw", "%" + req.getKeyword().trim() + "%");
                }
            }

            case "NAME" -> {
                sql.append(" AND LOWER(u.name) LIKE LOWER(:kw) ");
                params.put("kw", "%" + req.getKeyword().trim() + "%");
            }

            case "EMAIL" -> {
                sql.append(" AND LOWER(u.email) LIKE LOWER(:kw) ");
                params.put("kw", "%" + req.getKeyword().trim() + "%");
            }

            case "STATUS" -> {
                sql.append(" AND u.status = :status ");
                params.put("status", req.getStatus());
            }

            case "AGE_RANGE" -> {
                if (req.getAgeFrom() != null) {
                    sql.append(" AND u.age >= :ageFrom ");
                    params.put("ageFrom", req.getAgeFrom());
                }
                if (req.getAgeTo() != null) {
                    sql.append(" AND u.age <= :ageTo ");
                    params.put("ageTo", req.getAgeTo());
                }
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported searchType: " + type
            );
        }

        // =====================================================
        // ✅ 2. Safe ORDER BY
        // =====================================================
        String sortColumn = mapSortColumn(req.getSortBy());
        String sortDir = "ASC".equalsIgnoreCase(req.getSortDir())
                ? "ASC" : "DESC";

        sql.append(" ORDER BY ")
                .append(sortColumn)
                .append(" ")
                .append(sortDir);

        // =====================================================
        // ✅ 3. OFFSET / FETCH (Oracle 21c)
        // =====================================================
        int page = req.getPage() != null ? req.getPage() : 0;
        int size = req.getSize() != null ? req.getSize() : 10;
        int offset = page * size;

        sql.append("""
                    OFFSET :offset ROWS
                    FETCH NEXT :size ROWS ONLY
                """);

        // =====================================================
        // ✅ 4. Execute Native Query
        // =====================================================
        Query query = em.createNativeQuery(sql.toString());

        params.forEach(query::setParameter);
        query.setParameter("offset", offset);
        query.setParameter("size", size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<UserResponseDto> content = rows.stream()
                .map(this::mapRowToDto)
                .toList();

        // =====================================================
        // ✅ 5. COUNT query dùng lại switch
        // =====================================================
        String countSql = "SELECT COUNT(1) FROM (" +
                sql.substring(0, sql.indexOf("ORDER BY")) +
                ") c";

        Query countQuery = em.createNativeQuery(countSql);
        params.forEach(countQuery::setParameter);

        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(
                content,
                PageRequest.of(page, size),
                total
        );
    }

}
