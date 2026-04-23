package org.superwindcloud.fkill;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Fkill {
    private static final long ALIVE_CHECK_MIN_INTERVAL_MS = 5;
    private static final long ALIVE_CHECK_MAX_INTERVAL_MS = 1280;
    private static final Pattern WINDOWS_NETSTAT_PID = Pattern.compile(
        "^\\s*(TCP|UDP)\\s+\\S+:(\\d+)\\s+\\S+(?:\\s+\\S+)?\\s+(\\d+)\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SS_PID_PATTERN = Pattern.compile("pid=(\\d+)");

    private Fkill() {
    }

    public static void kill(String input) throws FkillException {
        kill(List.of(input), Options.builder().build());
    }

    public static void kill(Collection<String> inputs, Options options) throws FkillException {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(options, "options");

        Map<String, ResolvedTarget> resolvedTargets = new LinkedHashMap<>();
        for (String input : inputs) {
            resolvedTargets.put(input, resolveTarget(input, options));
        }

        List<String> errors = new ArrayList<>();
        Set<Long> allResolvedPids = new LinkedHashSet<>();

        for (Map.Entry<String, ResolvedTarget> entry : resolvedTargets.entrySet()) {
            String originalInput = entry.getKey();
            ResolvedTarget target = entry.getValue();

            if (target.pids().isEmpty()) {
                errors.add("Killing process " + originalInput + " failed: Process doesn't exist");
                continue;
            }

            allResolvedPids.addAll(target.pids());

            for (long pid : target.pids()) {
                try {
                    killPid(pid, target.tree(), options.force());
                } catch (Exception exception) {
                    errors.add("Killing process " + originalInput + " failed: " + cleanError(exception));
                }
            }
        }

        if (!errors.isEmpty() && !options.silent()) {
            throw new FkillException("Failed to kill processes", errors);
        }

        if (!options.force() && options.forceAfterTimeout() != null) {
            Set<Long> alive = waitUntilTimeout(allResolvedPids, options.forceAfterTimeout());
            for (long pid : alive) {
                try {
                    killPid(pid, options.tree(), true);
                } catch (Exception ignored) {
                    // Meaningful errors have already been reported during the first pass.
                }
            }
        }

        if (options.waitForExit() != null && !options.waitForExit().isNegative() && !options.waitForExit().isZero()) {
            Set<Long> alive = waitUntilTimeout(allResolvedPids, options.waitForExit());
            if (!alive.isEmpty() && !options.silent()) {
                List<String> waitErrors = alive.stream()
                    .map(pid -> "Process " + pid + " did not exit within " + options.waitForExit().toMillis() + "ms")
                    .toList();
                throw new FkillException("Processes did not exit within timeout", waitErrors);
            }
        }
    }

    private static ResolvedTarget resolveTarget(String input, Options options) throws FkillException {
        if (input == null || input.isBlank()) {
            throw new FkillException("Process input cannot be empty");
        }

        if (input.startsWith(":")) {
            int port = parsePort(input);
            return new ResolvedTarget(input, resolvePort(port), options.tree());
        }

        OptionalLongResult pid = parsePid(input);
        if (pid.present()) {
            return new ResolvedTarget(input, Set.of(pid.value()), options.tree());
        }

        return new ResolvedTarget(input, resolveByName(input, options.ignoreCase()), options.tree());
    }

    private static int parsePort(String input) throws FkillException {
        try {
            int port = Integer.parseInt(input.substring(1));
            if (port < 1 || port > 65535) {
                throw new FkillException("Port must be between 1 and 65535: " + input);
            }

            return port;
        } catch (NumberFormatException exception) {
            throw new FkillException("Invalid port input: " + input, exception);
        }
    }

    private static OptionalLongResult parsePid(String input) {
        try {
            return OptionalLongResult.of(Long.parseLong(input));
        } catch (NumberFormatException exception) {
            return OptionalLongResult.empty();
        }
    }

    private static Set<Long> resolveByName(String input, boolean ignoreCase) {
        Set<Long> protectedPids = currentAndParentPids();
        String normalizedInput = normalize(input, ignoreCase);
        boolean hasExtension = input.contains(".");

        Set<Long> matches = new LinkedHashSet<>();
        ProcessHandle.allProcesses().forEach(process -> {
            long pid = process.pid();
            if (protectedPids.contains(pid)) {
                return;
            }

            String executableName = process.info().command()
                .map(Fkill::fileName)
                .orElse("");
            if (executableName.isBlank()) {
                return;
            }

            String normalizedExecutable = normalize(executableName, ignoreCase);
            String executableWithoutExtension = stripExecutableExtension(normalizedExecutable);

            boolean matched = normalizedExecutable.equals(normalizedInput)
                || (!hasExtension && executableWithoutExtension.equals(normalizedInput));

            if (matched) {
                matches.add(pid);
            }
        });

        return matches;
    }

    private static Set<Long> currentAndParentPids() {
        Set<Long> pids = new HashSet<>();
        ProcessHandle current = ProcessHandle.current();
        pids.add(current.pid());
        current.parent().ifPresent(parent -> {
            ProcessHandle cursor = parent;
            while (cursor != null) {
                pids.add(cursor.pid());
                cursor = cursor.parent().orElse(null);
            }
        });
        return pids;
    }

    private static Set<Long> resolvePort(int port) throws FkillException {
        if (isWindows()) {
            return resolvePortOnWindows(port);
        }

        Set<Long> lsofResult = resolvePortWithLsof(port);
        if (!lsofResult.isEmpty()) {
            return lsofResult;
        }

        return resolvePortWithSs(port);
    }

    private static Set<Long> resolvePortOnWindows(int port) throws FkillException {
        CommandResult result = runCommand(List.of("netstat", "-ano"));
        if (result.exitCode() != 0) {
            throw new FkillException("Failed to inspect ports with netstat: " + result.stderr());
        }

        Set<Long> pids = new LinkedHashSet<>();
        for (String line : result.stdout().split("\\R")) {
            Matcher matcher = WINDOWS_NETSTAT_PID.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            int localPort = Integer.parseInt(matcher.group(2));
            if (localPort == port) {
                pids.add(Long.parseLong(matcher.group(3)));
            }
        }

        pids.remove(ProcessHandle.current().pid());
        return pids;
    }

    private static Set<Long> resolvePortWithLsof(int port) throws FkillException {
        CommandResult result = runCommand(List.of("lsof", "-nP", "-i:" + port, "-t"));
        if (result.exitCode() != 0 && result.exitCode() != 1 && result.stderr().contains("not found")) {
            throw new FkillException("`lsof` doesn't seem to be installed and is required to resolve ports");
        }

        return parsePidLines(result.stdout());
    }

    private static Set<Long> resolvePortWithSs(int port) throws FkillException {
        CommandResult result = runCommand(List.of("ss", "-lptn", "sport = :" + port));
        if (result.exitCode() != 0 && result.stderr().contains("not found")) {
            throw new FkillException("Neither `lsof` nor `ss` is available to resolve ports");
        }

        Set<Long> pids = new LinkedHashSet<>();
        for (String line : result.stdout().split("\\R")) {
            Matcher matcher = SS_PID_PATTERN.matcher(line);
            while (matcher.find()) {
                pids.add(Long.parseLong(matcher.group(1)));
            }
        }
        return pids;
    }

    private static Set<Long> parsePidLines(String stdout) {
        Set<Long> pids = new LinkedHashSet<>();
        for (String line : stdout.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            try {
                pids.add(Long.parseLong(trimmed));
            } catch (NumberFormatException ignored) {
                // Ignore unrelated lines from fallback commands.
            }
        }
        return pids;
    }

    private static void killPid(long pid, boolean tree, boolean force) throws FkillException {
        if (pid == ProcessHandle.current().pid()) {
            return;
        }

        ProcessHandle handle = ProcessHandle.of(pid)
            .orElseThrow(() -> new FkillException("Process doesn't exist"));

        if (tree) {
            List<ProcessHandle> descendants = handle.descendants().toList();
            for (int index = descendants.size() - 1; index >= 0; index--) {
                terminate(descendants.get(index), force);
            }
        }

        terminate(handle, force);
    }

    private static void terminate(ProcessHandle handle, boolean force) throws FkillException {
        if (!handle.isAlive()) {
            return;
        }

        boolean signalled = force ? handle.destroyForcibly() : handle.destroy();
        if (!signalled && handle.isAlive()) {
            throw new FkillException("Failed to signal process " + handle.pid());
        }
    }

    private static Set<Long> waitUntilTimeout(Set<Long> pids, Duration timeout) {
        long remaining = timeout.toMillis();
        long interval = Math.min(ALIVE_CHECK_MIN_INTERVAL_MS, Math.max(remaining, 0));
        Set<Long> alive = new LinkedHashSet<>(pids);

        while (remaining > 0 && !alive.isEmpty()) {
            sleep(interval);
            alive.removeIf(pid -> !ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
            remaining -= interval;
            interval = Math.min(interval * 2, ALIVE_CHECK_MAX_INTERVAL_MS);
            interval = Math.min(interval, Math.max(remaining, 0));
        }

        alive.removeIf(pid -> !ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
        return alive;
    }

    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for process exit", exception);
        }
    }

    private static CommandResult runCommand(List<String> command) throws FkillException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);

        try {
            Process process = processBuilder.start();
            byte[] stdoutBytes = process.getInputStream().readAllBytes();
            byte[] stderrBytes = process.getErrorStream().readAllBytes();
            int exitCode = process.waitFor();
            return new CommandResult(
                exitCode,
                new String(stdoutBytes, StandardCharsets.UTF_8),
                new String(stderrBytes, StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            return new CommandResult(127, "", exception.getMessage() == null ? exception.toString() : exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FkillException("Interrupted while running command: " + String.join(" ", command), exception);
        }
    }

    private static String cleanError(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    static String normalize(String value, boolean ignoreCase) {
        return ignoreCase ? value.toLowerCase(Locale.ROOT) : value;
    }

    static String stripExecutableExtension(String executable) {
        int dotIndex = executable.lastIndexOf('.');
        if (dotIndex <= 0) {
            return executable;
        }

        String extension = executable.substring(dotIndex + 1);
        return switch (extension) {
            case "exe", "cmd", "bat", "com", "sh" -> executable.substring(0, dotIndex);
            default -> executable;
        };
    }

    private static String fileName(String commandPath) {
        int slash = Math.max(commandPath.lastIndexOf('/'), commandPath.lastIndexOf('\\'));
        return slash >= 0 ? commandPath.substring(slash + 1) : commandPath;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private record ResolvedTarget(String originalInput, Set<Long> pids, boolean tree) {
    }

    private record CommandResult(int exitCode, String stdout, String stderr) {
    }

    public record Options(
        boolean force,
        boolean tree,
        boolean ignoreCase,
        boolean silent,
        Duration forceAfterTimeout,
        Duration waitForExit
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private boolean force;
            private boolean tree = true;
            private boolean ignoreCase;
            private boolean silent;
            private Duration forceAfterTimeout;
            private Duration waitForExit;

            private Builder() {
            }

            public Builder force(boolean force) {
                this.force = force;
                return this;
            }

            public Builder tree(boolean tree) {
                this.tree = tree;
                return this;
            }

            public Builder ignoreCase(boolean ignoreCase) {
                this.ignoreCase = ignoreCase;
                return this;
            }

            public Builder silent(boolean silent) {
                this.silent = silent;
                return this;
            }

            public Builder forceAfterTimeout(Duration forceAfterTimeout) {
                this.forceAfterTimeout = forceAfterTimeout;
                return this;
            }

            public Builder waitForExit(Duration waitForExit) {
                this.waitForExit = waitForExit;
                return this;
            }

            public Options build() {
                return new Options(force, tree, ignoreCase, silent, forceAfterTimeout, waitForExit);
            }
        }
    }

    private record OptionalLongResult(boolean present, long value) {

        static OptionalLongResult of(long value) {
                return new OptionalLongResult(true, value);
            }

            static OptionalLongResult empty() {
                return new OptionalLongResult(false, 0);
            }
        }
}
