

1. ✅ Tạo **index trên 4 cột (composite index)**
2. ✅ Các **kiểu query để Oracle sử dụng được index**
3. ⚠️ Các **trường hợp KHÔNG dùng được index**
4. ✅ Cách **kiểm tra execution plan**

---

## 1️⃣ Ví dụ Bảng Dữ Liệu

Giả sử có bảng `ORDERS`:

```sql
CREATE TABLE ORDERS (
    ORDER_ID    NUMBER PRIMARY KEY,
    CUSTOMER_ID NUMBER,
    STATUS      VARCHAR2(20),
    ORDER_DATE  DATE,
    AMOUNT      NUMBER
);
```

---

## 2️⃣ Tạo Index Trên 4 Cột

Tạo **composite index 4 cột** theo đúng thứ tự:

```sql
CREATE INDEX IDX_ORDERS_4COL
ON ORDERS (CUSTOMER_ID, STATUS, ORDER_DATE, AMOUNT);
```

✅ Thứ tự rất quan trọng:

```
(CUSTOMER_ID → STATUS → ORDER_DATE → AMOUNT)
```

---

## 3️⃣ Các Truy Vấn SẼ DÙNG ĐƯỢC Index

### ✅ 1. Dùng đầy đủ 4 cột → Tối ưu nhất

```sql
SELECT *
FROM ORDERS
WHERE CUSTOMER_ID = 1001
  AND STATUS = 'PAID'
  AND ORDER_DATE = DATE '2025-11-01'
  AND AMOUNT = 500000;
```

✔️ Oracle dùng **INDEX RANGE SCAN / UNIQUE SCAN**

---

### ✅ 2. Dùng 3 cột liên tiếp từ bên trái

```sql
SELECT *
FROM ORDERS
WHERE CUSTOMER_ID = 1001
  AND STATUS = 'PAID'
  AND ORDER_DATE >= DATE '2025-11-01';
```

✔️ Vẫn dùng được index
✔️ Bỏ cột AMOUNT **vẫn OK**

---

### ✅ 3. Dùng 2 cột đầu

```sql
SELECT *
FROM ORDERS
WHERE CUSTOMER_ID = 1001
  AND STATUS = 'PAID';
```

✔️ Dùng **INDEX RANGE SCAN**

---

### ✅ 4. Chỉ dùng cột đầu tiên

```sql
SELECT *
FROM ORDERS
WHERE CUSTOMER_ID = 1001;
```

✔️ Vẫn dùng index

---

### ✅ 5. Like có thể dùng index (nếu không bắt đầu bằng %)

```sql
SELECT *
FROM ORDERS
WHERE CUSTOMER_ID = 1001
  AND STATUS LIKE 'PA%';
```

✔️ Dùng index

---

## 4️⃣ Các Truy Vấn KHÔNG DÙNG ĐƯỢC Index

### ❌ 1. Bỏ cột đầu tiên (vi phạm nguyên tắc **LEFT-MOST PREFIX**)

```sql
SELECT *
FROM ORDERS
WHERE STATUS = 'PAID'
  AND ORDER_DATE = DATE '2025-11-01';
```

❌ **KHÔNG dùng index**
→ Vì thiếu `CUSTOMER_ID`

---

### ❌ 2. Dùng AMOUNT đơn lẻ

```sql
SELECT *
FROM ORDERS
WHERE AMOUNT = 500000;
```

❌ Không dùng index

---

### ❌ 3. Dùng hàm trên cột

```sql
SELECT *
FROM ORDERS
WHERE TO_CHAR(ORDER_DATE,'YYYY-MM-DD') = '2025-11-01';
```

❌ Oracle **không dùng index thường**
✔️ Muốn dùng → phải tạo **Function-Based Index**

---

### ❌ 4. Dùng toán tử `<>`, `!=`

```sql
SELECT *
FROM ORDERS
WHERE CUSTOMER_ID <> 1001;
```

❌ Không hiệu quả index → Full Table Scan

---

## 5️⃣ Trường Hợp DÙNG Index Một Phần (Partial)

```sql
SELECT *
FROM ORDERS
WHERE CUSTOMER_ID = 1001
  AND ORDER_DATE = DATE '2025-11-01';
```

⛔ Bị **skip STATUS**
→ Oracle **chỉ dùng được CUSTOMER_ID**, các cột sau bị vô hiệu

---

## 6️⃣ Kiểm Tra Oracle Có DÙNG Index Hay Không

### ✅ Cách 1: Explain Plan

```sql
EXPLAIN PLAN FOR
SELECT *
FROM ORDERS
WHERE CUSTOMER_ID = 1001
  AND STATUS = 'PAID';

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```

Nếu thấy:

```
INDEX RANGE SCAN IDX_ORDERS_4COL
```

→ ✅ INDEX ĐANG ĐƯỢC DÙNG

---

### ✅ Cách 2: Bật trace runtime

```sql
SELECT /*+ INDEX(ORDERS IDX_ORDERS_4COL) */ *
FROM ORDERS
WHERE CUSTOMER_ID = 1001
  AND STATUS = 'PAID';
```

---

## 7️⃣ Khi Nào Nên Tạo Index 4 Cột?

✅ Nên khi:

* Câu query **luôn lọc theo CUSTOMER_ID**
* Dữ liệu lớn ( > 1 triệu row )
* Dùng nhiều điều kiện chính xác (`=`)

❌ Không nên khi:

* Query chỉ lọc theo STATUS, AMOUNT
* Bảng nhỏ
* Insert/Update liên tục (index 4 cột rất nặng ghi)

---

## 8️⃣ Quy Tắc VÀNG Cho Index Nhiều Cột (Oracle)

| Quy Tắc             | Ý Nghĩa                                     |
| ------------------- | ------------------------------------------- |
| LEFT-MOST PREFIX    | Phải dùng cột đầu thì mới dùng được cột sau |
| = trước, RANGE sau  | Cột `=` để trước `BETWEEN`, `>=`            |
| Cột Selectivity cao | Đặt trước                                   |
| Tránh function      | Nếu cần → Function Index                    |

---

Nếu bạn muốn, tôi có thể:
✅ Viết **ví dụ Function-Based Index 4 cột**
✅ Phân tích **Execution Plan thực tế**
✅ Tối ưu index theo **bài toán hệ thống của bạn (ERP, Bank, Log, Transaction, Audit)**


