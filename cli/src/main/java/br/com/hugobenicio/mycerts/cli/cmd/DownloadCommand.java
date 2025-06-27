package br.com.hugobenicio.mycerts.cli.cmd;

import br.com.hugobenicio.mycerts.core.CertificateAnalyzer;
import br.com.hugobenicio.mycerts.core.LoadingCertificateException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Command(
        name = "download",
        description = "Download related subcommand"
)
public class DownloadCommand implements Runnable {

    @Option(
            names = {"--host"},
            description = "The server's hostname",
            required = true
    )
    private String host;

    @Option(
            names = {"--port"},
            description = "The server's port",
            required = false,
            defaultValue = "443",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    private Integer port;

    @Option(
            names = {"--sni"},
            description = "Server Name Indication (SNI)",
            required = false
    )
    private String sni;

    @Option(
            names = {"--output", "-o"},
            description = "Output file where the Truststore will be created",
            required = true
    )
    private File outputFile;

    @Option(
            names = {"--password"},
            required = true,
            interactive = true,
            defaultValue = "changeit",
            description = "used keystore password",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
            echo = false
    )
    private String password;

    @Override
    public void run() {

        // if sni is not given, use sni = host
        String serverNameIndicator = Optional.ofNullable(this.sni)
                .filter(String::isBlank)
                .map(String::trim)
                .orElse(this.host);

        var certificateAnalyzer = new CertificateAnalyzer();
        try {
            certificateAnalyzer.loadCertificatesFromRemoteServer(this.host, this.port, serverNameIndicator);
            certificateAnalyzer.saveCertsToFile(this.outputFile, this.password);
            //certificateAnalyzer.expirationReport();
        } catch (LoadingCertificateException | IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("truststore generated successfully. path=\"%s\"%n", this.outputFile);
    }
}
