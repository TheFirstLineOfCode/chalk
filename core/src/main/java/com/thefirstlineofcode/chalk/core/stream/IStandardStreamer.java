package com.thefirstlineofcode.chalk.core.stream;

import com.thefirstlineofcode.chalk.core.stream.negotiants.tls.IPeerCertificateTruster;

public interface IStandardStreamer extends IStreamer {
	void setPeerCertificateTruster(IPeerCertificateTruster certificateTruster);
	IPeerCertificateTruster getPeerCertificateTruster();
}
