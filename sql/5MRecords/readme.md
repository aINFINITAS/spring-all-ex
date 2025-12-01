# Spring + Oracle 5M Records

## SEARCH (OFFSET / LIMIT) → PROCESS → UPSERT (MERGE BATCH)

> Batch import 5M records with **OFFSET / LIMIT pagination**, **business processing**, and **MERGE batch UPSERT** in
Oracle (12c+).

---

## 1. Table Schema

```sql
-- SOURCE TABLE
CREATE TABLE SOURCE_CUSTOMER (
    ID     NUMBER PRIMARY KEY,
    NAME   VARCHAR2(255),
    EMAIL  VARCHAR2(255)
);

-- TARGET TABLE
CREATE TABLE CUSTOMER (
    ID          NUMBER PRIMARY KEY,
    NAME        VARCHAR2(255),
    EMAIL       VARCHAR2(255),
    UPDATED_AT  TIMESTAMP
);
```

---

## 2. DTO

```java
public class Customer {

    private Long id;
    private String name;
    private String email;

    public Customer(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
}
```

---

## 3. SEARCH Repository (OFFSET / LIMIT – Oracle 12c+)

```java
@Repository
@RequiredArgsConstructor
public class SourceCustomerRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public long count() {
        return jdbc.getJdbcTemplate()
                .queryForObject("SELECT COUNT(*) FROM SOURCE_CUSTOMER", Long.class);
    }

    private static final String PAGE_SQL = """
        SELECT ID, NAME, EMAIL
        FROM SOURCE_CUSTOMER
        ORDER BY ID
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
    """;

    public List<Customer> findPage(long offset, int limit) {

        Map<String, Object> params = Map.of(
                "offset", offset,
                "limit", limit
        );

        return jdbc.query(PAGE_SQL, params, (rs, i) ->
                new Customer(
                        rs.getLong("ID"),
                        rs.getString("NAME"),
                        rs.getString("EMAIL")
                )
        );
    }
}
```

---

## 4. TARGET Repository (Oracle MERGE Batch UPSERT)

```java
@Repository
@RequiredArgsConstructor
public class CustomerBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String MERGE_SQL = """
        MERGE INTO CUSTOMER t
        USING (
            SELECT ? AS ID,
                   ? AS NAME,
                   ? AS EMAIL
            FROM DUAL
        ) src
        ON (t.ID = src.ID)
        WHEN MATCHED THEN
            UPDATE SET
                t.NAME       = src.NAME,
                t.EMAIL      = src.EMAIL,
                t.UPDATED_AT = SYSTIMESTAMP
        WHEN NOT MATCHED THEN
            INSERT (ID, NAME, EMAIL, UPDATED_AT)
            VALUES (src.ID, src.NAME, src.EMAIL, SYSTIMESTAMP)
    """;

    public void upsertBatch(List<Customer> customers) {

        jdbcTemplate.batchUpdate(
            MERGE_SQL,
            customers,
            5_000,
            (ps, c) -> {
                ps.setLong(1, c.getId());
                ps.setString(2, c.getName());
                ps.setString(3, c.getEmail());
            }
        );
    }
}
```

---

## 5. PROCESS Layer (Business Processing)

```java
@Component
public class CustomerProcessor {

    public Customer process(Customer input) {

        String name = input.getName() != null
                ? input.getName().trim()
                : null;

        String email = input.getEmail() != null
                ? input.getEmail().toLowerCase().trim()
                : null;

        return new Customer(
                input.getId(),
                name,
                email
        );
    }
}
```

---

## 6. SERVICE – Full Flow: SEARCH → PROCESS → BATCH MERGE

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerMigrationService {

    private final SourceCustomerRepository sourceRepo;
    private final CustomerBatchRepository targetRepo;
    private final CustomerProcessor customerProcessor;

    private static final int PAGE_SIZE  = 100_000; // fetch per page
    private static final int BATCH_SIZE = 5_000;   // merge per batch

    public void migrate5M() {

        long total = sourceRepo.count();
        long totalPages = (long) Math.ceil((double) total / PAGE_SIZE);

        log.info("START MIGRATION | total={} | pages={}", total, totalPages);

        for (int page = 0; page < totalPages; page++) {

            long offset = (long) page * PAGE_SIZE;

            // 1. SEARCH
            List<Customer> rawPage =
                    sourceRepo.findPage(offset, PAGE_SIZE);

            if (rawPage.isEmpty()) {
                break;
            }

            log.info("Page {}/{} fetched {} records (offset={})",
                    page + 1, totalPages, rawPage.size(), offset);

            // 2. PROCESS
            List<Customer> processed = rawPage.stream()
                    .map(customerProcessor::process)
                    .toList();

            // 3. WRITE (BATCH MERGE)
            for (int i = 0; i < processed.size(); i += BATCH_SIZE) {
                int toIndex = Math.min(i + BATCH_SIZE, processed.size());
                List<Customer> batch = processed.subList(i, toIndex);

                targetRepo.upsertBatch(batch);
            }

            log.info("Page {}/{} processed & merged ({} records)",
                    page + 1, totalPages, processed.size());
        }

        log.info("MIGRATION COMPLETED");
    }
}
```

---

## 7. REST API Trigger

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/migration")
public class MigrationController {

    private final CustomerMigrationService migrationService;

    @PostMapping("/customers")
    public ResponseEntity<String> migrate() {
        migrationService.migrate5M(); // should be async in production
        return ResponseEntity.accepted().body("Migration started");
    }
}
```

---

## 8. Async Executor (Optional – Recommended)

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("importExecutor")
    public Executor importExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(200);
        ex.initialize();
        return ex;
    }
}
```

---

## 9. application.yml Optimization

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
  jdbc:
    template:
      fetch-size: 10000
```

JDBC URL:

```
jdbc:oracle:thin:@//host:1521/orcl
```

---

## 10. Execution Flow Summary

```
COUNT SOURCE
FOR EACH PAGE (OFFSET / LIMIT)
    -> SEARCH
    -> PROCESS
    -> BATCH MERGE (5k)
END
```

---

## 11. Production Notes

* Do not wrap entire migration in a single @Transactional
* Each batch should be an independent transaction
* Disable heavy indexes during massive import if possible
* Prefer keyset pagination if dataset > 20M

---


logging:
level:
org.springframework.transaction: TRACE

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void upsertOneBatch(List<Customer> batch) {
targetRepo.upsertBatch(batch);
}

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerMigrationService {

    @Async("migrationExecutor")
    public void migrate5MAsync() {
        migrate5M();
    }

    public void migrate5M() {
        // logic search → process → batch merge 5M record
    }

}

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("migrationExecutor")
    public Executor migrationExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(50);
        ex.initialize();
        return ex;
    }

}


