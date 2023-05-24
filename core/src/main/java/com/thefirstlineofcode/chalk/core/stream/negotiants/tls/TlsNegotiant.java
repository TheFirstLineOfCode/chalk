package com.thefirstlineofcode.chalk.core.stream.negotiants.tls;

import java.security.cert.Certificate;
import java.util.List;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.parsers.SimpleObjectParserFactory;
import com.thefirstlineofcode.basalt.oxm.translators.core.stream.tls.StartTlsTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Feature;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.basalt.xmpp.core.stream.tls.Failure;
import com.thefirstlineofcode.basalt.xmpp.core.stream.tls.Proceed;
import com.thefirstlineofcode.basalt.xmpp.core.stream.tls.StartTls;
import com.thefirstlineofcode.chalk.core.stream.INegotiationContext;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamer;
import com.thefirstlineofcode.chalk.core.stream.negotiants.InitialStreamNegotiant;
import com.thefirstlineofcode.chalk.network.ConnectionException;

public class TlsNegotiant extends InitialStreamNegotiant {
	protected static final int DEFAULT_TLS_PROCESS_TIMEOUT = 1000 * 15;
	protected static IOxmFactory oxmFactory = OxmService.createStreamOxmFactory();
	private IPeerCertificateTruster certificateTruster;
	
	static {
		oxmFactory.register(StartTls.class, new StartTlsTranslatorFactory());
		
		oxmFactory.register(ProtocolChain.first(Proceed.PROTOCOL),
				new SimpleObjectParserFactory<>(
						Proceed.PROTOCOL,
						Proceed.class)
				);
		oxmFactory.register(ProtocolChain.first(Failure.PROTOCOL),
				new SimpleObjectParserFactory<>(
						Failure.PROTOCOL,
						Failure.class)
				);
	}
	
	private boolean tlsPreferred;
	
	public TlsNegotiant(String hostName, String lang, boolean tlsPreferred) {
		super(hostName, lang);
		this.tlsPreferred = tlsPreferred;
	}
	
	@Override
	protected void doNegotiate(INegotiationContext context) throws ConnectionException, NegotiationException {
		@SuppressWarnings("unchecked")
		List<Feature> features = (List<Feature>)context.getAttribute(StandardStreamer.NEGOTIATION_KEY_FEATURES);
		StartTls startTls = findStartTls(features);
		
		if (startTls != null) {
			if (startTls.getRequired() || tlsPreferred) {
				negotiateTls(context);
				super.doNegotiate(context);
			}
		}
	}

	protected void negotiateTls(INegotiationContext context) throws ConnectionException, NegotiationException {
		StartTls startTls = new StartTls();
		context.write(oxmFactory.translate(startTls));
		
		Object response = oxmFactory.parse(readResponse(DEFAULT_TLS_PROCESS_TIMEOUT));
		if (response instanceof Proceed) {
			doStartTls(context);
		} else if (response instanceof Failure) {
			response = oxmFactory.parse(readResponse(DEFAULT_READ_RESPONSE_TIMEOUT));
			
			if (response instanceof Stream) {
				Stream stream = (Stream)response;
				if (stream.isClose()) {
					context.close();
				}
			}
			
			throw new NegotiationException(this);
		} else {
			processError((IError)response, context, oxmFactory);
		}
	}

	protected void doStartTls(INegotiationContext context) throws ConnectionException {
		Certificate[] certificates = context.startTls();
		if (certificateTruster != null && !certificateTruster.accept(certificates)) {
			throw new PeerCertificateTrustException("untrusted peer certificates");
		}
	}

	private StartTls findStartTls(List<Feature> features) {
		for (Feature feature : features) {
			if (feature instanceof StartTls)
				return (StartTls)feature;
		}
		
		return null;
	}
	
	public void setPeerCertificateTruster(IPeerCertificateTruster certificateTruster) {
		this.certificateTruster = certificateTruster;
	}
	
	public IPeerCertificateTruster getPeerCertificateTruster() {
		return certificateTruster;
	}
}
