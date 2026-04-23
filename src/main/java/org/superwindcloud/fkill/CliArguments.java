package org.superwindcloud.fkill;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CliArguments {
  private final List<String> inputs;
  private final Fkill.Options options;
  private final boolean help;

  public CliArguments(List<String> inputs, Fkill.Options options, boolean help) {
    this.inputs = List.copyOf(inputs);
    this.options = options;
    this.help = help;
  }

  public List<String> inputs() {
    return inputs;
  }

  public Fkill.Options options() {
    return options;
  }

  public boolean help() {
    return help;
  }

  public static CliArguments parse(String[] args) {
    List<String> inputs = new ArrayList<>();
    Fkill.Options.Builder builder = Fkill.Options.builder();
    boolean help = false;

    for (int index = 0; index < args.length; index++) {
      String argument = args[index];
      if (!argument.startsWith("-") || "-".equals(argument)) {
        inputs.add(argument);
        continue;
      }

      switch (argument) {
        case "-h", "--help" -> help = true;
        case "-f", "--force" -> builder.force(true);
        case "-i", "--ignore-case" -> builder.ignoreCase(true);
        case "-s", "--silent" -> builder.silent(true);
        case "-t", "--tree" -> builder.tree(true);
        case "--no-tree" -> builder.tree(false);
        default -> {
          if (argument.startsWith("--force-after-timeout=")) {
            builder.forceAfterTimeout(
                parseDuration(
                    argument.substring(argument.indexOf('=') + 1), "force-after-timeout"));
          } else if (argument.equals("--force-after-timeout")) {
            index = requireValue(args, index, argument);
            builder.forceAfterTimeout(parseDuration(args[index], "force-after-timeout"));
          } else if (argument.startsWith("--wait-for-exit=")) {
            builder.waitForExit(
                parseDuration(argument.substring(argument.indexOf('=') + 1), "wait-for-exit"));
          } else if (argument.equals("--wait-for-exit")) {
            index = requireValue(args, index, argument);
            builder.waitForExit(parseDuration(args[index], "wait-for-exit"));
          } else {
            throw new IllegalArgumentException("Unknown option: " + argument);
          }
        }
      }
    }

    return new CliArguments(inputs, builder.build(), help);
  }

  private static int requireValue(String[] args, int index, String option) {
    int nextIndex = index + 1;
    if (nextIndex >= args.length) {
      throw new IllegalArgumentException("Missing value for option: " + option);
    }

    return nextIndex;
  }

  private static Duration parseDuration(String raw, String optionName) {
    try {
      long milliseconds = Long.parseLong(raw);
      if (milliseconds < 0) {
        throw new IllegalArgumentException("Option --" + optionName + " must be >= 0");
      }

      return Duration.ofMillis(milliseconds);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(
          "Option --" + optionName + " expects milliseconds: " + raw, exception);
    }
  }

  public static String usage() {
    return """
            Usage: java -jar killport-1.0-SNAPSHOT.jar [options] <pid|name|:port>...

            Options:
              -f, --force                    Force kill the process.
              -i, --ignore-case              Ignore case when matching process names.
              -s, --silent                   Ignore per-target failures.
              -t, --tree                     Kill child processes too.
                  --no-tree                  Kill only the matched process.
                  --force-after-timeout <ms> Retry with force after timeout.
                  --wait-for-exit <ms>       Wait until process exits.
              -h, --help                     Show this help.

            Inputs:
              1234        Kill process by PID
              java        Kill processes by executable name
              :8080       Kill process bound to port 8080
            """;
  }
}
