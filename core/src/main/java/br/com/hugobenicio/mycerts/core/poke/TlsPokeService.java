package br.com.hugobenicio.mycerts.core.poke;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TlsPokeService {

    public static final int PORT_DEFAULT = 443;

    private final String host;

    private final int port;

    public TlsPokeService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public TlsPokeService(String host) {
        this(host, PORT_DEFAULT);
    }

    /// Pokes a server to validate a TLS handshake.
    public void poke() throws IOException {
        var sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(this.host, this.port)) {

            InputStream in = sslsocket.getInputStream();
            OutputStream out = sslsocket.getOutputStream();

            // Write a test byte to get a reaction :)
            out.write(0x0);

            while (in.available() > 0) {
                System.out.print(in.read());
            }
        }
        System.out.println("Successfully connected");
    }
}
