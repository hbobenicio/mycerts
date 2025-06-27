package br.com.hugobenicio.mycerts.core;

import br.com.hugobenicio.mycerts.core.pem.PemBlock;
import br.com.hugobenicio.mycerts.core.pem.PemSplitter;
import br.com.hugobenicio.mycerts.core.tls.InsecureX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.lang.String.format;

public class CertificateAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(CertificateAnalyzer.class);

    public static final List<String> SUPPORTED_FILE_EXTENSIONS = List.of( "pem", "jks", "p12");

    /**
     * Insecure TLS Socket Factory
     */
    private final SSLSocketFactory sslSocketFactory = createSslSocketFactory();
    private static SSLSocketFactory createSslSocketFactory() {
        try {
            SSLContext insecureTlsContext = SSLContext.getInstance("TLS");

            //NOTE(security): we intentionally use an insecure trust manager here because we're interested in analyzing
            //                server's certificates even if we do not have a preset trusted cert chain.
            insecureTlsContext.init(null, InsecureX509TrustManager.newTrustManagers(), new SecureRandom());

            return insecureTlsContext.getSocketFactory();

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Certificate factory used to generate X509 Certificates
     */
    private final CertificateFactory x509CertificateFactory = createX509CertificateFactory();
    private static CertificateFactory createX509CertificateFactory() {
        try {
            return CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private final KeyStore jksKeyStore = createJksKeyStore();
    private static KeyStore createJksKeyStore() {
        try {
            return KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            throw new AssertionError(e);
        }
    }

    private final KeyStore p12KeyStore = createP12KeyStore();
    private static KeyStore createP12KeyStore() {
        try {
            return KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            throw new AssertionError(e);
        }
    }

    private final PemSplitter pemSplitter = new PemSplitter();

    private final List<X509Certificate> certificates = new ArrayList<>();

    public CertificateAnalyzer() {
    }

    public void loadCertificatesFromRemoteServer(String host) throws LoadingCertificateException {
        loadCertificatesFromRemoteServer(host, 443, host);
    }

    public void loadCertificatesFromRemoteServer(String host, int port) throws LoadingCertificateException {
        loadCertificatesFromRemoteServer(host, port, host);
    }

    public void loadCertificatesFromRemoteServer(String host, String tlsSniServerName) throws LoadingCertificateException {
        loadCertificatesFromRemoteServer(host, 443, tlsSniServerName);
    }

    /**
     * Connects to a remote server, performs the TLS handshake and then just collects its peer certificates then
     * closes the connection.
     *
     * @param host Hostname of the server
     * @param port Port of the server
     * @param tlsSniServerName Servername used by the server (TLS SNI)
     * @throws LoadingCertificateException If anything fails
     */
    public void loadCertificatesFromRemoteServer(String host, int port, String tlsSniServerName)
            throws LoadingCertificateException {

        log.atDebug().setMessage("creating socket to server..")
                .addKeyValue("host", host)
                .addKeyValue("port", port)
                .log();
        try (var sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port)) {
            log.atDebug().setMessage("socket created")
                    .addKeyValue("host", host)
                    .addKeyValue("port", port)
                    .log();

            // sets the Server Name Indication (SNI) TLS Extension
            var sslParams = new SSLParameters();
            sslParams.setServerNames(List.of(new SNIHostName(tlsSniServerName)));
            sslSocket.setSSLParameters(sslParams);

            log.atDebug().setMessage("performing tls handshake with the server..")
                    .addKeyValue("host", host)
                    .addKeyValue("port", port)
                    .addKeyValue("sni", tlsSniServerName)
                    .log();
            sslSocket.startHandshake();

            log.atDebug().setMessage("tls handshake success")
                    .addKeyValue("host", host)
                    .addKeyValue("port", port)
                    .addKeyValue("sni", tlsSniServerName)
                    .log();

            var peerCertificates = (X509Certificate[]) sslSocket.getSession().getPeerCertificates();
            log.atDebug().setMessage("session cipher suite")
                .addKeyValue("cipher", sslSocket.getSession().getCipherSuite())
                .log();

            Collections.addAll(this.certificates, peerCertificates);

        } catch (IOException e) {
            var msg = format("failed to load certificates from server. host=%s port=%d server_name=%s",
                    host, port, tlsSniServerName);
            throw new LoadingCertificateException(msg, e);
        }
    }

    /**
     * Reads a file and parses it as PEM encoded data that may contain x509 blocks in it.
     *
     * @param pemFilePath the input PEM file path
     * @throws LoadingCertificateException if anything fails
     */
    public void loadCertificatesFromPemFile(Path pemFilePath) throws LoadingCertificateException {
        final String fileContents;
        try {
            fileContents = Files.readString(pemFilePath);
        } catch (IOException e) {
            var msg = format("Failed to load certificates from file. path=\"%s\"", pemFilePath.getFileName());
            throw new LoadingCertificateException(msg, e);
        }

        List<PemBlock> pemBlocks = this.pemSplitter.split(fileContents);
        for (var block: pemBlocks) {
            String pemType = block.name();
            if ("PKCS7".equalsIgnoreCase(pemType)) {
                log.warn("pkcs7 blocks inside pem files are not supported atm. ignoring it for now");
                continue;
            }

            if (!"CERTIFICATE".equalsIgnoreCase(pemType)) {
                log.warn("unsupported block inside pem file. type={}", pemType);
                continue;
            }

            byte[] pemBytes = block.fullData().getBytes();
            try (InputStream is = new ByteArrayInputStream(pemBytes)) {
                var crt = (X509Certificate) x509CertificateFactory.generateCertificate(is);
                this.certificates.add(crt);
            } catch (IOException | CertificateException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void loadCertificatesFromJksInputStream(InputStream is, String password) throws LoadingCertificateException {
        char[] passwordBytes = Optional.ofNullable(password).map(String::toCharArray).orElse(null);
        loadCertificatesFromJksOrP12InputStream(jksKeyStore, is, passwordBytes);
    }

    public void loadCertificatesFromP12InputStream(InputStream is, String password) throws LoadingCertificateException {
        char[] passwordBytes = Optional.ofNullable(password).map(String::toCharArray).orElse(null);
        loadCertificatesFromJksOrP12InputStream(p12KeyStore, is, passwordBytes);
    }

    private void loadCertificatesFromJksOrP12InputStream(KeyStore ks, InputStream is, char[] password) throws LoadingCertificateException {
        try {
            ks.load(is, password);

            // iterating over all jks entries
            for (var aliases = ks.aliases(); aliases.hasMoreElements(); ) {
                String alias = aliases.nextElement();
                Certificate certificate = ks.getCertificate(alias);
                if (!(certificate instanceof X509Certificate)) {
                    log.warn("ignoring certificate with alias \"{}\" because it doesn't seems to be a X509 certificate", alias);
                    continue;
                }
                certificates.add((X509Certificate) certificate);
            }
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new LoadingCertificateException("Failed to load certificates from file", e);
        }
    }

    /**
     * Sorts the loaded certificates by increasing NotAfter date (expires sooner first)
     */
    public void sortCertificatesByNotAfterDateAscending() {
        this.certificates.sort((c1, c2) -> {
            Date c1NotAfter = c1.getNotAfter();
            Date c2NotAfter = c2.getNotAfter();

            // if c1 has expiration date older than the c2 expiration date
            if (c1NotAfter.before(c2NotAfter)) {
                return -1;
            }

            // if c1 has expiration date newer than the c2 expiration date
            if (c1NotAfter.after(c2NotAfter)) {
                return 1;
            }

            return 0;
        });
    }

    public void individualReport() {
        for (int i = 0; i < certificates.size(); i++) {
            X509Certificate certificate = certificates.get(i);

            Date notAfter = certificate.getNotAfter();
            String name = certificate.getSubjectX500Principal().getName();

            System.out.printf("Certificate #%d:%n", i+1);
            System.out.printf("  Name: %s%n", name);
            System.out.printf("  NotAfter: %s%n", notAfter);
            System.out.println();
        }
    }

    /**
     * Just a simple and practical stdout report of the loaded certificates
     */
    public void expirationReport() {
        log.info("analyzing {} certificates(s) for expiration report..", certificates.size());

        List<X509Certificate> expiredCertificates = new ArrayList<>();
        List<X509Certificate> expiringWithin7Days = new ArrayList<>();
        List<X509Certificate> expiringWithin30Days = new ArrayList<>();
        List<X509Certificate> expiringWithin90Days = new ArrayList<>();
        List<X509Certificate> expiringWithin180Days = new ArrayList<>();
        List<X509Certificate> notExpiringSoon = new ArrayList<>();

        Instant now = Instant.now();
        Instant ahead7Days = now.plus(7, ChronoUnit.DAYS);
        Instant ahead30Days = now.plus(30, ChronoUnit.DAYS);
        Instant ahead90Days = now.plus(90, ChronoUnit.DAYS);
        Instant ahead180Days = now.plus(180, ChronoUnit.DAYS);

        for (var certificate: certificates) {
            Date notAfter = certificate.getNotAfter();
            Instant notAfterInstant = notAfter.toInstant();

            if (notAfterInstant.isBefore(now)) {
                expiredCertificates.add(certificate);
                continue;
            }

            if (notAfterInstant.isBefore(ahead7Days)) {
                expiringWithin7Days.add(certificate);
                continue;
            }

            if (notAfterInstant.isBefore(ahead30Days)) {
                expiringWithin30Days.add(certificate);
                continue;
            }

            if (notAfterInstant.isBefore(ahead90Days)) {
                expiringWithin90Days.add(certificate);
                continue;
            }

            if (notAfterInstant.isBefore(ahead180Days)) {
                expiringWithin180Days.add(certificate);
                continue;
            }

            notExpiringSoon.add(certificate);
        }

        System.out.println("Expiration Report:");
        System.out.printf("    Expired count: %d%n", expiredCertificates.size());
        expirationReportDetails(expiredCertificates, now);
        System.out.printf("    Expiring within next   7 days count: %d%n", expiringWithin7Days.size());
        expirationReportDetails(expiringWithin7Days, now);
        System.out.printf("    Expiring within next  30 days count: %d%n", expiringWithin30Days.size());
        expirationReportDetails(expiringWithin30Days, now);
        System.out.printf("    Expiring within next  90 days count: %d%n", expiringWithin90Days.size());
        expirationReportDetails(expiringWithin90Days, now);
        System.out.printf("    Expiring within next 180 days count: %d%n", expiringWithin180Days.size());
        expirationReportDetails(expiringWithin180Days, now);
        System.out.printf("    Not expiring soon count............: %d%n", notExpiringSoon.size());
        expirationReportDetails(notExpiringSoon, now);

        log.info("expiration report done");
    }

    public void saveCertsToFile(File outputFile, String password) throws IOException {
        //TODO check if file already exists (error if it does)
        char[] passwordCharArray = password.toCharArray();
        try {
            // creates a new keystore for the giving password
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, passwordCharArray);

            // import all loaded certificates into it
            for (var certificate: this.certificates) {
                String entryName = certificate.getSubjectX500Principal().getName();
                ks.setCertificateEntry( entryName, certificate);
            }

            // save it to the giving file
            try (var fos = new FileOutputStream(outputFile)) {
                try {
                    ks.store(fos, passwordCharArray);
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    private void expirationReportDetails(List<X509Certificate> certificates, Instant now) {
        for (final var certificate: certificates) {
            String name = certificate.getSubjectX500Principal().getName();
            Instant notAfterInstant = certificate.getNotAfter().toInstant();
            var expirationDuration = Duration.between(now, notAfterInstant);

            final String pronoun = expirationDuration.isNegative() ? "since" : "in";
            expirationDuration = expirationDuration.abs();

            var expirationDays = expirationDuration.toDaysPart();
            var expirationMinutes = expirationDuration.toMinutesPart();
            var expirationSeconds = expirationDuration.toSecondsPart();
            var expirationMsg = format("%s %d days, %d minutes, %d seconds", pronoun, expirationDays, expirationMinutes, expirationSeconds);
            System.out.printf("        %s (%s): %s%n", notAfterInstant, expirationMsg, name);
        }
    }

    public static boolean isFileExtensionSupported(String fileExtension) {
        return SUPPORTED_FILE_EXTENSIONS.contains(fileExtension);
    }

    public List<X509Certificate> getCertificates() {
        return certificates;
    }
}
