# killport

[中文](README.zh.md)

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

### Complete API Example

```java
import java.time.Duration;
import java.util.List;
import org.superwindcloud.fkill.Fkill;
import org.superwindcloud.fkill.FkillException;

public class Demo {
    public static void main(String[] args) throws FkillException {
        // 1. Simplest case: kill the process listening on a port.
        Fkill.kill(":8080");

        // 2. Advanced case: resolve and kill multiple targets in one call.
        Fkill.Options options = Fkill.Options.builder()
            .ignoreCase(true)
            .tree(true)
            .silent(false)
            .forceAfterTimeout(Duration.ofMillis(1000))
            .waitForExit(Duration.ofMillis(3000))
            .build();

        try {
            Fkill.kill(List.of(":8081", "java", "12345"), options);
            System.out.println("All matching processes terminated.");
        } catch (FkillException exception) {
            System.err.println(exception.getMessage());
            for (String error : exception.errors()) {
                System.err.println(" - " + error);
            }
        }
    }
}
```

This example covers three target types:

- `:8081`: kill by port
- `java`: kill by process name
- `12345`: kill by PID

Common integration patterns:

```java
Fkill.kill(":3000");
Fkill.kill("4321");
Fkill.kill(List.of(":8080", "node"), Fkill.Options.builder().ignoreCase(true).build());
```

See `examples/LibraryUsageExample.java` for a runnable example.

If you want a complete backend sample project, see `demo-springboot/`.

### Spring Boot Integration Example

If you want to expose this as a backend API, wrap the library in your own service instead of calling it directly from a controller everywhere.

```java
package com.example.demo.service;

import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;
import org.superwindcloud.fkill.Fkill;
import org.superwindcloud.fkill.FkillException;

@Service
public class ProcessKillService {

    public void killByPort(int port) throws FkillException {
        Fkill.kill(":" + port);
    }

    public void killTargets(List<String> targets) throws FkillException {
        Fkill.Options options = Fkill.Options.builder()
                .ignoreCase(true)
                .tree(true)
                .silent(false)
                .forceAfterTimeout(Duration.ofMillis(1000))
                .waitForExit(Duration.ofMillis(3000))
                .build();

        Fkill.kill(targets, options);
    }
}
```

Request DTO:

```java
package com.example.demo.web;

import java.util.List;

public record KillRequest(
        List<String> targets,
        Boolean ignoreCase,
        Boolean tree,
        Boolean silent,
        Long forceAfterTimeoutMs,
        Long waitForExitMs) {}
```

Controller example:

```java
package com.example.demo.web;

import java.time.Duration;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.superwindcloud.fkill.Fkill;
import org.superwindcloud.fkill.FkillException;

@RestController
@RequestMapping("/api/processes")
public class ProcessKillController {
    @PostMapping("/kill")
    public ResponseEntity<?> kill(@RequestBody KillRequest request) {
        try {
            Fkill.Options options = Fkill.Options.builder()
                    .ignoreCase(Boolean.TRUE.equals(request.ignoreCase()))
                    .tree(request.tree() == null || request.tree())
                    .silent(Boolean.TRUE.equals(request.silent()))
                    .forceAfterTimeout(
                            request.forceAfterTimeoutMs() == null
                                    ? null
                                    : Duration.ofMillis(request.forceAfterTimeoutMs()))
                    .waitForExit(
                            request.waitForExitMs() == null
                                    ? null
                                    : Duration.ofMillis(request.waitForExitMs()))
                    .build();

            List<String> targets = request.targets() == null ? List.of() : request.targets();
            Fkill.kill(targets, options);

            return ResponseEntity.ok().body("Processes terminated.");
        } catch (FkillException exception) {
            return ResponseEntity.badRequest().body(exception.errors().isEmpty()
                    ? exception.getMessage()
                    : exception.errors());
        }
    }
}
```

Example request:

```http
POST /api/processes/kill
Content-Type: application/json

{
  "targets": [":8080", "java", "12345"],
  "ignoreCase": true,
  "tree": true,
  "silent": false,
  "forceAfterTimeoutMs": 1000,
  "waitForExitMs": 3000
}
```

Notes:

- `targets` supports port, process name, and PID in one request
- if `tree` is omitted, defaulting to `true` is usually the safer behavior
- do not expose this endpoint publicly without strict authentication/authorization
- in production, add allowlists and audit logging

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

Option meanings:

- `force`: terminate forcefully from the start
- `tree`: terminate child processes as well, default is `true`
- `ignoreCase`: ignore case when matching process names
- `silent`: suppress per-target failures
- `forceAfterTimeout`: retry with force after graceful termination times out
- `waitForExit`: maximum time to wait for actual process exit

## Implementation Notes

- On Windows, port-to-process resolution uses `netstat -ano`
- On Linux and macOS, it tries `lsof` first and falls back to `ss`
- Process name matching is based on `ProcessHandle.info().command()`
- To avoid killing itself, the current Java process and its parent chain are skipped

## Known Limitations

- Process name matching depends on whether the JVM can read the target process command path, which may vary by OS and permissions
- On Unix-like systems, port resolution will not work if neither `lsof` nor `ss` is available
- This implementation follows the core behavior of `fkill`, but does not reproduce every edge case from the Node.js version
