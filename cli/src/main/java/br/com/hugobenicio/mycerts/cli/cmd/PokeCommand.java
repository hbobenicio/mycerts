package br.com.hugobenicio.mycerts.cli.cmd;

import br.com.hugobenicio.mycerts.core.poke.TlsPokeService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "poke", description = "Pokes a server to test SSL connectivity (aka SSLPoke)")
public class PokeCommand implements Callable<Integer> {

    @Option(
            names = {"--host"},
            required = true,
            description = "The server's hostname"
    )
    private String host;

    @Option(
            names = {"--port"},
            required = false,
            defaultValue = "443",
            description = "The server's port"
    )
    private Integer port;

    @Override
    public Integer call() throws IOException {
        var tlsPokeService = new TlsPokeService(this.host, this.port);
        tlsPokeService.poke();
        return 0;
    }
}
