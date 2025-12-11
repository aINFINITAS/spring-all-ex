package com.example.demo.auth;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

@Service
public class PermissionService {

    private static final Map<String, Set<String>> PERMISSION_MAP = Map.of(
            "A", Set.of("P1"),
            "B", Set.of("P2"),
            "ADMIN", Set.of("P1", "P2", "P3"),
            "MANAGER", Set.of("P6", "P5")
    );

    public boolean hasPermission(String type, String[] requiredPerms) {
        Set<String> userPerms = PERMISSION_MAP.get(type);
        if (userPerms == null)
            return false;

        return Arrays.stream(requiredPerms)
                .anyMatch(userPerms::contains);
    }

    public boolean typeExists(String type) {
        return PERMISSION_MAP.containsKey(type);
    }
}

