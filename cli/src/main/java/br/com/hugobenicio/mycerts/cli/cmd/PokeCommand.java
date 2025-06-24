package br.com.hugobenicio.mycerts.cli.cmd;

import picocli.CommandLine.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Command(name = "poke", description = "Pokes a server to test SSL connectivity (aka SSLPoke)")
public class PokeCommand implements Runnable {

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
    public void run() {
        var sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(this.host, this.port)) {

            InputStream in = sslsocket.getInputStream();
            OutputStream out = sslsocket.getOutputStream();

            // Write a test byte to get a reaction :)
            out.write(1);

            while (in.available() > 0) {
                System.out.print(in.read());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Successfully connected");
    }
}
