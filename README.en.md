# killport

A Java implementation inspired by [`sindresorhus/fkill`](https://github.com/sindresorhus/fkill). It can be used both as a command-line tool and as a reusable library in Java projects.

Features:

- Kill a process by `PID`
- Kill processes by name
- Kill the process listening on `:port`
- Kill the whole child process tree
- Force kill after a timeout
- Wait for process exit
- Ignore per-target failures

## Build

```bash
mvn package
```

The built artifact is generated at:

```text
target/killport-1.0-SNAPSHOT.jar
```

## CLI Usage

Show help:

```bash
java -jar target/killport-1.0-SNAPSHOT.jar --help
```

Kill by PID:

```bash
java -jar target/killport-1.0-SNAPSHOT.jar 1234
```

Kill by port:

```bash
java -jar target/killport-1.0-SNAPSHOT.jar :8080
```

Kill by process name and ignore case:

```bash
java -jar target/killport-1.0-SNAPSHOT.jar --ignore-case java
```

Force kill after a timeout and wait for exit:

```bash
java -jar target/killport-1.0-SNAPSHOT.jar --force-after-timeout 1000 --wait-for-exit 3000 java
```

Available options:

- `-f, --force`: forcefully terminate the process
- `-i, --ignore-case`: ignore case when matching process names
- `-s, --silent`: ignore per-target failures
- `-t, --tree`: terminate child processes too
- `--no-tree`: terminate only the matched process
- `--force-after-timeout <ms>`: retry with force after the timeout
- `--wait-for-exit <ms>`: wait until the process exits
- `-h, --help`: show help

## Using As A Library

This project is a standard Maven `jar`, so it can be used directly as a dependency.

### Maven Dependency

If you publish it to a private repository or install it locally, you can depend on it like this:

```xml
<dependency>
    <groupId>org.superwindcloud</groupId>
    <artifactId>killport</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

For local development, install it into your local Maven repository first:

```bash
mvn install
```

### API Example

```java
import java.time.Duration;
import java.util.List;
import org.superwindcloud.fkill.Fkill;
import org.superwindcloud.fkill.FkillException;

public class Demo {
    public static void main(String[] args) throws FkillException {
        Fkill.Options options = Fkill.Options.builder()
            .ignoreCase(true)
            .tree(true)
            .forceAfterTimeout(Duration.ofMillis(1000))
            .waitForExit(Duration.ofMillis(3000))
            .build();

        Fkill.kill(List.of(":8080", "java"), options);
    }
}
```

See `examples/LibraryUsageExample.java` for a complete example.

## Public API

Main entry points:

- `Fkill.kill(String input)`
- `Fkill.kill(Collection<String> inputs, Fkill.Options options)`

Exception type:

- `FkillException`

Configurable options:

- `force`
- `tree`
- `ignoreCase`
- `silent`
- `forceAfterTimeout`
- `waitForExit`

## Implementation Notes

- On Windows, port-to-process resolution uses `netstat -ano`
- On Linux and macOS, it tries `lsof` first and falls back to `ss`
- Process name matching is based on `ProcessHandle.info().command()`
- To avoid killing itself, the current Java process and its parent chain are skipped

## Known Limitations

- Process name matching depends on whether the JVM can read the target process command path, which may vary by OS and permissions
- On Unix-like systems, port resolution will not work if neither `lsof` nor `ss` is available
- This implementation follows the core behavior of `fkill`, but does not reproduce every edge case from the Node.js version
