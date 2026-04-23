package org.superwindcloud.fkill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FkillTest {
    @Test
    void stripsCommonExecutableExtensions() {
        assertEquals("java", Fkill.stripExecutableExtension("java.exe"));
        assertEquals("gradle", Fkill.stripExecutableExtension("gradle.bat"));
        assertEquals("node", Fkill.stripExecutableExtension("node"));
    }

    @Test
    void normalizesCaseWhenRequested() {
        assertEquals("java.exe", Fkill.normalize("Java.Exe", true));
        assertEquals("Java.Exe", Fkill.normalize("Java.Exe", false));
    }
}
