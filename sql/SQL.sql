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
