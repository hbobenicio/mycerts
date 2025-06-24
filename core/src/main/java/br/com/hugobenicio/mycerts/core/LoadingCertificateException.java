package br.com.hugobenicio.mycerts.core;

public class LoadingCertificateException extends Exception {

    public LoadingCertificateException() {
        super();
    }

    public LoadingCertificateException(String message) {
        super(message);
    }

    public LoadingCertificateException(String message, Throwable cause) {
        super(message, cause);
    }
}
