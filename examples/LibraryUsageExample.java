import java.time.Duration;
import java.util.List;
import org.superwindcloud.fkill.Fkill;
import org.superwindcloud.fkill.FkillException;

public final class LibraryUsageExample {
    private LibraryUsageExample() {}

    public static void main(String[] args) {
        Fkill.Options options = Fkill.Options.builder()
                .ignoreCase(true)
                .tree(true)
                .forceAfterTimeout(Duration.ofMillis(1_000))
                .waitForExit(Duration.ofMillis(3_000))
                .build();

        try {
            Fkill.kill(List.of(":8080", "java"), options);
            System.out.println("Processes terminated.");
        } catch (FkillException exception) {
            System.err.println(exception.getMessage());
        }
    }
}
