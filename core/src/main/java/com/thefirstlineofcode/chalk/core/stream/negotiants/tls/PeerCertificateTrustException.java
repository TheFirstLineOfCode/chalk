package com.thefirstlineofcode.chalk.core.stream.negotiants.tls;

public class PeerCertificateTrustException extends RuntimeException {

	private static final long serialVersionUID = 8960751450000514099L;

	public PeerCertificateTrustException() {
		super();
	}

	public PeerCertificateTrustException(String message, Throwable cause) {
		super(message, cause);
	}

	public PeerCertificateTrustException(String message) {
		super(message);
	}

	public PeerCertificateTrustException(Throwable cause) {
		super(cause);
	}

}
