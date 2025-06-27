package br.com.hugobenicio.mycerts.cli.cmd;

import picocli.AutoComplete;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * <p>CLI Design</p>
 *
 * $ mycerts download --host=www.google.com
 * $ mycerts download --host=www.google.com --port=443
 * $ mycerts poke --host=www.google.com --port=443
 */
@Command(
        name = "mycerts",
        subcommands = {
                DownloadCommand.class,
                PokeCommand.class,
                AutoComplete.GenerateCompletion.class,
                HelpCommand.class,
        }
)
public class RootCommand {}
