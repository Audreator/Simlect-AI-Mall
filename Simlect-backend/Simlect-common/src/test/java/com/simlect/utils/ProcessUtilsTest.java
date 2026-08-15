package com.simlect.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class ProcessUtilsTest {

    @Test
    void executeCommand_emptyOrNullCommand_returnsNull() {
        assertNull(ProcessUtils.executeCommand(null, false));
        assertNull(ProcessUtils.executeCommand("", false));
    }
}
