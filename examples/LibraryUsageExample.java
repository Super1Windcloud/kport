import java.time.Duration;
import java.util.List;
import org.superwindcloud.fkill.Fkill;
import org.superwindcloud.fkill.FkillException;

public final class LibraryUsageExample {
    private LibraryUsageExample() {}

    public static void main(String[] args) {
        killBySinglePort();
        killMultipleTargets();
    }

    private static void killBySinglePort() {
        try {
            Fkill.kill(":8080");
            System.out.println("Process on port 8080 terminated.");
        } catch (FkillException exception) {
            System.err.println("Single-target kill failed: " + exception.getMessage());
        }
    }

    private static void killMultipleTargets() {
        Fkill.Options options = Fkill.Options.builder()
                .ignoreCase(true)
                .tree(true)
                .silent(false)
                .forceAfterTimeout(Duration.ofMillis(1_000))
                .waitForExit(Duration.ofMillis(3_000))
                .build();

        List<String> targets = List.of(
                ":8081",
                "java",
                "12345");

        try {
            Fkill.kill(targets, options);
            System.out.println("All matching processes terminated.");
        } catch (FkillException exception) {
            System.err.println("Batch kill failed: " + exception.getMessage());
            if (!exception.errors().isEmpty()) {
                exception.errors().forEach(error -> System.err.println(" - " + error));
            }
        }
    }
}
