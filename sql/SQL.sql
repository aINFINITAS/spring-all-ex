String
sql = """
    SELECT id, name, email, country, last_login
    FROM users
    WHERE active = COALESCE(:active, active)
      AND country = COALESCE(:country, country)
      AND age >= COALESCE(:minAge, age)
      AND last_login >= COALESCE(:lastLogin, last_login)
""";

Map
<String, Object> params = Map.of(
    "active", null,
    "country", "VN",
    "minAge", 20,
    "lastLogin", null
);

//
👉 Tạo SQL hoàn chỉnh chỉ để log
String finalSql = sql
        .replace(":active", params.get("active") == null ? "NULL" : params.get("active").toString())
        .replace(":country", "'" + params.get("country") + "'")
        .replace(":minAge", params.get("minAge").toString())
        .replace(":lastLogin", "NULL");

System.out.println
("🧾 Final SQL:\n" + finalSql);





CREATE
OR REPLACE VIEW v_users_status AS
SELECT u.id,
       u.name,
       u.email,
       u.status,
       CASE u.status
           WHEN 'A' THEN 'ACTIVE'
           WHEN 'I' THEN 'INACTIVE'
           WHEN 'B' THEN 'BLOCKED'
           ELSE 'UNKNOWN'
           END AS status_name,
       u.created_at
FROM users u;



CREATE
OR REPLACE VIEW v_users_age_group AS
SELECT u.id,
       u.name,
       u.age,
       CASE
           WHEN u.age < 18 THEN 'CHILD'
           WHEN u.age BETWEEN 18 AND 30 THEN 'YOUNG'
           WHEN u.age BETWEEN 31 AND 60 THEN 'ADULT'
           ELSE 'SENIOR'
           END AS age_group
FROM users u;

