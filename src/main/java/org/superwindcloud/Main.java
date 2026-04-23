package org.superwindcloud;

import java.util.List;
import org.superwindcloud.fkill.CliArguments;
import org.superwindcloud.fkill.Fkill;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            CliArguments cliArguments = CliArguments.parse(args);
            if (cliArguments.help()) {
                System.out.println(CliArguments.usage());
                return;
            }

            List<String> inputs = cliArguments.inputs();
            if (inputs.isEmpty()) {
                System.err.println("Missing process input.");
                System.err.println(CliArguments.usage());
                System.exit(1);
            }

            Fkill.kill(inputs, cliArguments.options());
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.err.println(CliArguments.usage());
            System.exit(1);
        } catch (Exception exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        }
    }
}
