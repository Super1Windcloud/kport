package org.superwindcloud.fkill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CliArgumentsTest {
    @Test
    void parsesFlagsAndInputs() {
        CliArguments arguments = CliArguments.parse(new String[]{
            "--force",
            "--ignore-case",
            "--force-after-timeout=200",
            "--wait-for-exit",
            "500",
            ":8080",
            "java"
        });

        assertEquals(2, arguments.inputs().size());
        assertTrue(arguments.options().force());
        assertTrue(arguments.options().ignoreCase());
        assertEquals(Duration.ofMillis(200), arguments.options().forceAfterTimeout());
        assertEquals(Duration.ofMillis(500), arguments.options().waitForExit());
    }

    @Test
    void supportsNoTreeFlag() {
        CliArguments arguments = CliArguments.parse(new String[]{"--no-tree", "1234"});
        assertFalse(arguments.options().tree());
    }

    @Test
    void rejectsUnknownOption() {
        assertThrows(IllegalArgumentException.class, () -> CliArguments.parse(new String[]{"--wat"}));
    }
}
