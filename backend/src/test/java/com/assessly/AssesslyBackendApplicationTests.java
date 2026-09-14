package com.assessly;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AssesslyBackendApplicationTests {
    @Test
    void applicationEntryPointIsLoadable() {
        assertDoesNotThrow(() -> Class.forName("com.assessly.AssesslyBackendApplication"));
    }
}
