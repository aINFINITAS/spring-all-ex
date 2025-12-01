

## Simple SEARCH → PROCESS → UPSERT (MERGE)

> Minimal Spring Batch implementation for **5M+ records** with:
>
> * Oracle 12c+
> * OFFSET/FETCH paging (via `JdbcPagingItemReader`)
> * Business processing
> * Batch MERGE UPSERT
>
> Flow: `READER → PROCESSOR → WRITER (MERGE)`

---

## 1. Maven Dependencies

```xml

<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-batch</artifactId>
</dependency>

<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<dependency>
<groupId>com.oracle.database.jdbc</groupId>
<artifactId>ojdbc11</artifactId>
</dependency>
```

---

## 2. Database Schema

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

## 3. DTO

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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
```

---

## 4. Spring Batch Configuration (Reader → Processor → Writer)

```java
@Configuration
@EnableBatchProcessing
public class CustomerBatchConfig {

    // =========================
    // ITEM READER (SEARCH)
    // =========================
    @Bean
    @StepScope
    public JdbcPagingItemReader<Customer> customerReader(DataSource dataSource) {

        OraclePagingQueryProvider queryProvider = new OraclePagingQueryProvider();

        queryProvider.setSelectClause("SELECT ID, NAME, EMAIL");
        queryProvider.setFromClause("FROM SOURCE_CUSTOMER");
        queryProvider.setSortKeys(Map.of("ID", Order.ASCENDING));

        return new JdbcPagingItemReaderBuilder<Customer>()
                .name("customerReader")
                .dataSource(dataSource)
                .queryProvider(queryProvider)
                .pageSize(10_000)   // READ 10k per page
                .rowMapper((rs, i) -> new Customer(
                        rs.getLong("ID"),
                        rs.getString("NAME"),
                        rs.getString("EMAIL")
                ))
                .build();
    }

    // =========================
    // ITEM PROCESSOR (BUSINESS LOGIC)
    // =========================
    @Bean
    public ItemProcessor<Customer, Customer> customerProcessor() {
        return customer -> {
            String name = customer.getName() != null
                    ? customer.getName().trim()
                    : null;

            String email = customer.getEmail() != null
                    ? customer.getEmail().toLowerCase().trim()
                    : null;

            return new Customer(customer.getId(), name, email);
        };
    }

    // =========================
    // ITEM WRITER (UPSERT MERGE)
    // =========================
    @Bean
    public JdbcBatchItemWriter<Customer> customerWriter(DataSource dataSource) {

        String mergeSql = """
            MERGE INTO CUSTOMER t
            USING (
                SELECT :id AS ID,
                       :name AS NAME,
                       :email AS EMAIL
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

        return new JdbcBatchItemWriterBuilder<Customer>()
                .dataSource(dataSource)
                .sql(mergeSql)
                .beanMapped()
                .build();
    }

    // =========================
    // STEP (CHUNK = BATCH SIZE)
    // =========================
    @Bean
    public Step customerStep(
            StepBuilderFactory stepBuilderFactory,
            ItemReader<Customer> reader,
            ItemProcessor<Customer, Customer> processor,
            ItemWriter<Customer> writer
    ) {
        return stepBuilderFactory.get("customerStep")
                .<Customer, Customer>chunk(5_000)   // 5k = 1 TRANSACTION
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    // =========================
    // JOB (SINGLE STEP)
    // =========================
    @Bean
    public Job customerMigrationJob(
            JobBuilderFactory jobBuilderFactory,
            Step customerStep
    ) {
        return jobBuilderFactory.get("customerMigrationJob")
                .start(customerStep)
                .build();
    }
}
```

---

## 5. REST API Trigger

```java

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/migration")
public class MigrationController {

    private final JobLauncher jobLauncher;
    private final Job customerMigrationJob;

    @PostMapping("/customers")
    public ResponseEntity<String> migrate() throws Exception {

        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(customerMigrationJob, params);

        return ResponseEntity.accepted()
                .body("Spring Batch migration job started");
    }
}
```

---

## 6. application.yml

```yaml
spring:
  batch:
    job:
      enabled: false
  datasource:
    hikari:
      maximum-pool-size: 10
```

---

## 7. Execution Flow

```
API /customers
   ↓
Spring Batch Job
   ↓
JdbcPagingItemReader   (READ 10k/page)
   ↓
ItemProcessor           (PROCESS)
   ↓
JdbcBatchItemWriter     (MERGE 5k/chunk)
   ↓
COMMIT
   ↓
LOOP UNTIL ~5M DONE
```

---

## 8. Transaction & Performance Notes

* Each `chunk(5000)` = 1 independent DB transaction
* Failure in one chunk → only that chunk is rolled back
* Spring Batch auto manages:

    * Commit / rollback
    * Restart on failure
    * Job metadata (`BATCH_JOB_*` tables)

---

## 9. Default Tuning for 5M Records

```
Reader pageSize  = 10_000
Writer chunkSize = 5_000
Total records    = 5_000_000
Total commits    ≈ 1_000
```

---
