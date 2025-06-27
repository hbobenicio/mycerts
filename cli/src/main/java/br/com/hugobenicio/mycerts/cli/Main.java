package br.com.hugobenicio.mycerts.cli;

import br.com.hugobenicio.mycerts.cli.cmd.RootCommand;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        var cli = new CommandLine(new RootCommand());
        cli.execute(args);
        // NOTE: If the command was `mycerts server start`, we can't call System.exit(cli.execute(args)) here because
        //       this command here is executed while the server thread is still starting/running.
        //       For more details, see
        //       https://stackoverflow.com/questions/76461848/picocli-with-long-running-javalin-thread
    }
}
