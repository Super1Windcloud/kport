package com.example.demo.service;

import com.example.demo.config.ProcessKillProperties;
import com.example.demo.web.KillRequest;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.superwindcloud.fkill.Fkill;
import org.superwindcloud.fkill.FkillException;

@Service
public class ProcessKillService {
  private final ProcessKillProperties properties;

  public ProcessKillService(ProcessKillProperties properties) {
    this.properties = properties;
  }

  public void validateToken(String token) {
    if (token == null || !token.equals(properties.getApiToken())) {
      throw new IllegalArgumentException("Invalid admin token");
    }
  }

  public void kill(KillRequest request) throws FkillException {
    List<String> targets = request.targets();
    if (targets == null || targets.isEmpty()) {
      throw new IllegalArgumentException("targets must not be empty");
    }

    validateTargets(targets, Boolean.TRUE.equals(request.ignoreCase()));

    Fkill.Options options =
        Fkill.Options.builder()
            .ignoreCase(Boolean.TRUE.equals(request.ignoreCase()))
            .tree(request.tree() == null || request.tree())
            .silent(Boolean.TRUE.equals(request.silent()))
            .forceAfterTimeout(toDuration(request.forceAfterTimeoutMs()))
            .waitForExit(toDuration(request.waitForExitMs()))
            .build();

    Fkill.kill(targets, options);
  }

  private void validateTargets(List<String> targets, boolean ignoreCase) {
    Set<String> allowedNames = normalizedAllowedNames(ignoreCase);
    for (String target : targets) {
      if (target == null || target.isBlank()) {
        throw new IllegalArgumentException("target must not be blank");
      }

      if (target.startsWith(":")) {
        int port = parsePort(target);
        if (!properties.getAllowedPorts().contains(port)) {
          throw new IllegalArgumentException("Port not allowed: " + port);
        }
        continue;
      }

      if (isNumeric(target)) {
        if (!properties.isAllowPid()) {
          throw new IllegalArgumentException("PID killing is disabled");
        }
        continue;
      }

      String normalizedTarget = normalize(target, ignoreCase);
      if (!allowedNames.contains(normalizedTarget)) {
        throw new IllegalArgumentException("Process name not allowed: " + target);
      }
    }
  }

  private Set<String> normalizedAllowedNames(boolean ignoreCase) {
    Set<String> result = new LinkedHashSet<>();
    for (String name : properties.getAllowedProcessNames()) {
      result.add(normalize(name, ignoreCase));
    }
    return result;
  }

  private static String normalize(String value, boolean ignoreCase) {
    return ignoreCase ? value.toLowerCase(Locale.ROOT) : value;
  }

  private static boolean isNumeric(String value) {
    for (int index = 0; index < value.length(); index++) {
      if (!Character.isDigit(value.charAt(index))) {
        return false;
      }
    }
    return !value.isEmpty();
  }

  private static int parsePort(String target) {
    try {
      int port = Integer.parseInt(target.substring(1));
      if (port < 1 || port > 65535) {
        throw new IllegalArgumentException("Invalid port: " + target);
      }
      return port;
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("Invalid port: " + target, exception);
    }
  }

  private static Duration toDuration(Long milliseconds) {
    if (milliseconds == null) {
      return null;
    }
    if (milliseconds < 0) {
      throw new IllegalArgumentException("Timeout must be >= 0");
    }
    return Duration.ofMillis(milliseconds);
  }
}
