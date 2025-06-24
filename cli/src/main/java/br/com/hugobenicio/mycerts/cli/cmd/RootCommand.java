package br.com.hugobenicio.mycerts.cli.cmd;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(
        name = "mycerts",
        subcommands = {
                DownloadCommand.class,
                PokeCommand.class,
                HelpCommand.class,
        }
)
public class RootCommand {}
