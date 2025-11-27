

---

````md
# Factory Pattern with Spring – Auto Injection using Set<PaymentService>

This example demonstrates a clean and extensible **Factory Design Pattern**
implementation in **Spring Boot**, using automatic injection of
`Set<PaymentService>` and a `getType()` method for dynamic strategy resolution.

This approach follows:
- ✅ Open–Closed Principle (OCP)
- ✅ Dependency Inversion Principle (DIP)
- ✅ Strategy + Factory Pattern
- ✅ No hard-coded implementations in the Factory

---

## 1. Payment Type Enum

```java
public enum PaymentType {
    MOMO,
    VNPAY,
    CASH
}
````

---

## 2. Strategy Interface

Each implementation must declare which `PaymentType` it supports.

```java
public interface PaymentService {

    // Returns the supported payment type
    PaymentType getType();

    // Executes the payment logic
    void pay(long amount);
}
```

---

## 3. Concrete Implementations

### 3.1 MOMO Payment

```java

@Service
public class MomoPaymentService implements PaymentService {

    @Override
    public PaymentType getType() {
        return PaymentType.MOMO;
    }

    @Override
    public void pay(long amount) {
        System.out.println("Paying with MOMO: " + amount);
    }
}
```

---

### 3.2 VNPAY Payment

```java

@Service
public class VnpayPaymentService implements PaymentService {

    @Override
    public PaymentType getType() {
        return PaymentType.VNPAY;
    }

    @Override
    public void pay(long amount) {
        System.out.println("Paying with VNPAY: " + amount);
    }
}
```

---

### 3.3 CASH Payment

```java

@Service
public class CashPaymentService implements PaymentService {

    @Override
    public PaymentType getType() {
        return PaymentType.CASH;
    }

    @Override
    public void pay(long amount) {
        System.out.println("Paying with CASH: " + amount);
    }
}
```

---

## 4. Factory Implementation (Auto-Build Map from Set)

Spring automatically injects all `PaymentService` beans into a `Set`.
The factory builds a lookup map dynamically at startup.

```java

@Component
public class PaymentFactory {

    private final Map<PaymentType, PaymentService> serviceMap;

    // Spring injects all PaymentService implementations here
    public PaymentFactory(Set<PaymentService> services) {
        this.serviceMap = services.stream()
                .collect(Collectors.toMap(
                        PaymentService::getType,     // Key = PaymentType
                        Function.identity(),        // Value = Implementation
                        (a, b) -> a,                 // Resolve duplicate keys
                        () -> new EnumMap<>(PaymentType.class)
                ));
    }

    // Returns the correct implementation based on type
    public PaymentService getService(PaymentType type) {
        PaymentService service = serviceMap.get(type);

        if (service == null) {
            throw new IllegalArgumentException(
                    "Unsupported payment type: " + type
            );
        }
        return service;
    }
}
```

---

## 5. Usage Example

```java

@Service
@RequiredArgsConstructor
public class OrderService {

    private final PaymentFactory paymentFactory;

    public void processPayment(PaymentType type, long amount) {
        PaymentService service = paymentFactory.getService(type);
        service.pay(amount);
    }
}
```
```java

public enum PaymentType {
    MOMO,
    VNPAY,
    CASH
}

```
---

## 6. Key Advantages

* No `if-else` or `switch` statements
* No manual injection of each implementation
* Automatically supports new payment types
* Fully compliant with SOLID principles
* Ideal for enterprise Spring applications

---

## 7. Flow Summary

1. Spring scans and registers all `PaymentService` implementations.
2. All implementations are injected into `Set<PaymentService>`.
3. Factory converts the `Set` into a `Map<PaymentType, PaymentService>`.
4. At runtime, the correct implementation is resolved by `PaymentType`.

---

