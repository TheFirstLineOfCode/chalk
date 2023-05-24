package com.thefirstlineofcode.chalk.core.stream.negotiants.tls;

import java.security.cert.Certificate;

public interface IPeerCertificateTruster {
	boolean accept(Certificate[] certificates);
}
