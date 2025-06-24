package br.com.hugobenicio.mycerts.cli.cmd;

import br.com.hugobenicio.mycerts.core.CertificateAnalyzer;
import br.com.hugobenicio.mycerts.core.LoadingCertificateException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.Optional;

@Command(
        name = "download",
        description = "Download related subcommand"
)
public class DownloadCommand implements Runnable {

    @Option(
            names = {"--host"},
            required = true,
            description = "The server's hostname",
            interactive = true
    )
    private String host;

    @Option(
            names = {"--port"},
            required = false,
            defaultValue = "443",
            description = "The server's port",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
            interactive = true
    )
    private Integer port;

    @Option(
            names = {"--sni"},
            required = false,
            //TODO how to set default value from another option?
            description = "Server Name Indication (SNI)"
    )
    private String sni;

    @Option(
            names = {"--password"},
            required = false,
            interactive = true,
            defaultValue = "changeit",
            description = "The keystore password",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    private String password;

    @Override
    public void run() {
        String serverNameIndicator = Optional.ofNullable(this.sni)
                .filter(String::isBlank)
                .map(String::trim)
                .orElse(this.host);

        var outputFilePath = "ca.truststore.jks";

        var certificateAnalyzer = new CertificateAnalyzer();
        try {
            certificateAnalyzer.loadCertificatesFromRemoteServer(this.host, this.port, serverNameIndicator);
            certificateAnalyzer.saveCertsToFile(outputFilePath, this.password);
        } catch (LoadingCertificateException | IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("truststore generated successfully. path=\"%s\"%n", outputFilePath);
    }
}
