
## 2. Application Main

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

---

## 3. Repository Layer

```java
package com.example.demo.repo;

import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    public String findUsernameById(Long id) {
        // Simulate real DB access
        return "real-user-" + id;
    }
}
```

---

## 4. Service Layer

```java
package com.example.demo.service;

import com.example.demo.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;

    public String getUsername(Long id) {
        return repo.findUsernameById(id);
    }

    public String getRole(Long id) {
        // Simulated real business logic
        return "USER";
    }
}
```

---

## 5. Controller Layer

```java
package com.example.demo.controller;

import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping("/{id}")
    public String getUser(@PathVariable Long id) {
        return service.getUsername(id);
    }
}
```

---

# =========================

# ========== TEST =========

# =========================

## 6. Unit Test – @Mock + @InjectMocks

File: `UserService_UnitTest_Mock_InjectMocks.java`

```java
package com.example.demo.service;

import com.example.demo.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserService_UnitTest_Mock_InjectMocks {

    @Mock
    UserRepository repo;

    @InjectMocks
    UserService service;

    @Test
    void test_getUsername_mock_repo() {
        when(repo.findUsernameById(1L))
                .thenReturn("mock-user");

        String result = service.getUsername(1L);

        assertEquals("mock-user", result);
    }
}
```

✅ Pure unit test
✅ Fast
❌ No Spring, no AOP, no Transaction

---

## 7. Unit Test – @Spy (Partial Mock)

File: `UserService_UnitTest_Spy.java`

```java
package com.example.demo.service;

import com.example.demo.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserService_UnitTest_Spy {

    @Mock
    UserRepository repo;

    @Spy
    @InjectMocks
    UserService service;

    @Test
    void test_spy_real_method_and_mock_one() {

        when(repo.findUsernameById(1L))
                .thenReturn("spy-user");

        doReturn("ADMIN")
                .when(service)
                .getRole(1L);

        String username = service.getUsername(1L);
        String role = service.getRole(1L);

        assertEquals("spy-user", username);
        assertEquals("ADMIN", role);
    }
}
```

✅ Real logic + override 1 method
⚠️ Dangerous if real method hits external systems

---

## 8. Integration Test – @MockBean (Controller + Spring Context)

File: `UserController_IT_MockBean.java`

```java
package com.example.demo.controller;

import com.example.demo.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserController_IT_MockBean {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserRepository repo;

    @Test
    void test_controller_with_mockbean() throws Exception {

        when(repo.findUsernameById(1L))
                .thenReturn("mockbean-user");

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("mockbean-user"));
    }
}
```

✅ Full Spring Context
✅ MVC + AOP + Security ready
❌ Slower than unit test

---

## 9. Integration Test – @SpyBean (Partial Mock with Spring)

File: `UserService_IT_SpyBean.java`

```java
package com.example.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
class UserService_IT_SpyBean {

    @SpyBean
    UserService service;

    @Test
    void test_spybean_real_context() {

        doReturn("SUPER_ADMIN")
                .when(service)
                .getRole(1L);

        String role = service.getRole(1L);

        assertEquals("SUPER_ADMIN", role);
    }
}
```

✅ Works with:

* `@Transactional`
* `@Cacheable`
* `@Async`
* AOP Proxy

❌ Slowest but closest to production behavior

---

# =========================

# ========== SUMMARY ======

# =========================

| Annotation     | Spring Context | Real Method | Purpose                  |
| -------------- | -------------- | ----------- | ------------------------ |
| `@Mock`        | ❌              | ❌           | Pure unit test           |
| `@InjectMocks` | ❌              | ❌           | Inject mock dependency   |
| `@Spy`         | ❌              | ✅           | Partial unit mock        |
| `@MockBean`    | ✅              | ❌           | Integration with Spring  |
| `@SpyBean`     | ✅              | ✅           | Partial integration mock |

---

# =========================

# ========== HOW TO RUN ===

# =========================

### Run all tests

```bash
mvn clean test
```

### Run a single test

```bash
mvn -Dtest=UserService_UnitTest_Mock_InjectMocks test
```

---

# =========================

# ========== ENTERPRISE RULES ==

# =========================

* Use `@Mock + @InjectMocks` for **Service Unit Test**
* Use `@MockBean` for **Controller & Security Test**
* Use `@SpyBean` for **Transactional / Cache / AOP Test**
* Avoid using `@SpringBootTest` for pure unit test
* Never allow `@Spy` to call real DB or external API

---

```
