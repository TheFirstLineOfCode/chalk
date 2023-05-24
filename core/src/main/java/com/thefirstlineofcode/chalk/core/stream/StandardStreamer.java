package com.thefirstlineofcode.chalk.core.stream;

import java.util.ArrayList;
import java.util.List;

import com.thefirstlineofcode.chalk.core.stream.negotiants.InitialStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.negotiants.ResourceBindingNegotiant;
import com.thefirstlineofcode.chalk.core.stream.negotiants.SessionEstablishmentNegotiant;
import com.thefirstlineofcode.chalk.core.stream.negotiants.sasl.SaslNegotiant;
import com.thefirstlineofcode.chalk.core.stream.negotiants.tls.IPeerCertificateTruster;
import com.thefirstlineofcode.chalk.core.stream.negotiants.tls.TlsNegotiant;
import com.thefirstlineofcode.chalk.network.IConnection;

public class StandardStreamer extends AbstractStreamer implements IStandardStreamer {
	protected IPeerCertificateTruster certificateTruster;
	
	public StandardStreamer(StandardStreamConfig streamConfig) {
		super(streamConfig);
	}
	
	public StandardStreamer(StandardStreamConfig streamConfig, IConnection connection) {
		super(streamConfig, connection);
	}

	protected List<IStreamNegotiant> createNegotiants() {
		List<IStreamNegotiant> negotiants = new ArrayList<>();
		
		InitialStreamNegotiant initialStreamNegotiant = createInitialStreamNegotiant();
		negotiants.add(initialStreamNegotiant);
		
		TlsNegotiant tls = createTlsNegotiant();
		negotiants.add(tls);
		
		SaslNegotiant sasl = createSaslNegotiant();
		negotiants.add(sasl);
		
		ResourceBindingNegotiant resourceBindingNegotiant = createResourceBindingNegotiant();
		negotiants.add(resourceBindingNegotiant);
		
		SessionEstablishmentNegotiant sessionEstablishmentNegotiant = createSessionEstablishmentNegotiant();
		negotiants.add(sessionEstablishmentNegotiant);
		
		setNegotiationReadResponseTimeout(negotiants);
		
		return negotiants;
	}

	protected SessionEstablishmentNegotiant createSessionEstablishmentNegotiant() {
		return new SessionEstablishmentNegotiant();
	}

	protected ResourceBindingNegotiant createResourceBindingNegotiant() {
		return new ResourceBindingNegotiant(((StandardStreamConfig)streamConfig).getResource());
	}

	protected SaslNegotiant createSaslNegotiant() {
		SaslNegotiant sasl = new SaslNegotiant(streamConfig.getHost(), streamConfig.getLang(), authToken);
		sasl.setAuthenticationCallback(authenticationCallback);
		return sasl;
	}

	protected TlsNegotiant createTlsNegotiant() {
		TlsNegotiant tls = new TlsNegotiant(streamConfig.getHost(), streamConfig.getLang(),
				((StandardStreamConfig)streamConfig).isTlsPreferred());
		tls.setPeerCertificateTruster(certificateTruster);
		return tls;
	}

	protected InitialStreamNegotiant createInitialStreamNegotiant() {
		return new InitialStreamNegotiant(streamConfig.getHost(), streamConfig.getLang());
	}
	
	@Override
	public void setPeerCertificateTruster(IPeerCertificateTruster certificateTruster) {
		this.certificateTruster = certificateTruster;
	}

	@Override
	public IPeerCertificateTruster getPeerCertificateTruster() {
		return certificateTruster;
	}
	
}
