package com.thefirstlineofcode.chalk.xeps.ibr;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import com.thefirstlineofcode.basalt.oxm.coc.CocParserFactory;
import com.thefirstlineofcode.basalt.xeps.ibr.Register;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Features;
import com.thefirstlineofcode.chalk.core.AbstractChatClient;
import com.thefirstlineofcode.chalk.core.stream.AbstractStreamer;
import com.thefirstlineofcode.chalk.core.stream.IStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.IStreamer;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;
import com.thefirstlineofcode.chalk.core.stream.negotiants.InitialStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.negotiants.tls.IPeerCertificateTruster;
import com.thefirstlineofcode.chalk.core.stream.negotiants.tls.TlsNegotiant;
import com.thefirstlineofcode.chalk.network.IConnection;

class IbrChatClient extends AbstractChatClient {
	private IPeerCertificateTruster peerCertificateTruster;

	public IbrChatClient(StreamConfig streamConfig) {
		super(streamConfig, null);
	}
	
	public IbrChatClient(StreamConfig streamConfig, IConnection connection) {
		super(streamConfig, connection);
	}
	
	public void setPeerCertificateTruster(IPeerCertificateTruster peerCertificateTruster) {
		this.peerCertificateTruster = peerCertificateTruster;
	}

	public IPeerCertificateTruster getPeerCertificateTruster() {
		return peerCertificateTruster;
	}

	@Override
	protected IStreamer createStreamer(StreamConfig streamConfig, IConnection connection) {
		IbrStreamer streamer = new IbrStreamer(getStreamConfig(), connection);
		streamer.setNegotiationListener(this);
		
		if (peerCertificateTruster != null) {
			streamer.setPeerCertificateTruster(peerCertificateTruster);
		} else {
			// always trust all peer certificates.
			streamer.setPeerCertificateTruster(new IPeerCertificateTruster() {				
				@Override
				public boolean accept(Certificate[] certificates) {
					return true;
				}
			});
		}
		
		return streamer;
	}

	private class IbrStreamer extends AbstractStreamer {
		private IPeerCertificateTruster certificateTruster;
		
		public IbrStreamer(StreamConfig streamConfig, IConnection connection) {
			super(streamConfig, connection);
		}
		
		@Override
		protected List<IStreamNegotiant> createNegotiants() {
			List<IStreamNegotiant> negotiants = new ArrayList<>();
			
			InitialStreamNegotiant initialStreamNegotiant = createIbrSupportedInitialStreamNegotiant();
			negotiants.add(initialStreamNegotiant);
			
			TlsNegotiant tls = createIbrSupportedTlsNegotiant();
			negotiants.add(tls);
			
			IbrNegotiant ibr = createIbrNegotiant();
			negotiants.add(ibr);
			
			setNegotiationReadResponseTimeout(negotiants);
			
			return negotiants;
		}

		private IbrNegotiant createIbrNegotiant() {
			return new IbrNegotiant();
		}
		
		public void setPeerCertificateTruster(IPeerCertificateTruster certificateTruster) {
			this.certificateTruster = certificateTruster;
		}

		private InitialStreamNegotiant createIbrSupportedInitialStreamNegotiant() {
			return new IbrSupportedInitialStreamNegotiant(streamConfig.getHost(), streamConfig.getLang());
		}
		
		private TlsNegotiant createIbrSupportedTlsNegotiant() {
			TlsNegotiant tls = new IbrSupportedTlsNegotiant(streamConfig.getHost(), streamConfig.getLang(),
					((StandardStreamConfig)streamConfig).isTlsPreferred());
			tls.setPeerCertificateTruster(certificateTruster);
			return tls;
		}
	}
	
	private static class IbrSupportedInitialStreamNegotiant extends InitialStreamNegotiant {
		
		static {
			oxmFactory.register(ProtocolChain.first(Features.PROTOCOL).next(Register.PROTOCOL),
					new CocParserFactory<>(Register.class));
		}

		public IbrSupportedInitialStreamNegotiant(String hostName, String lang) {
			super(hostName, lang);
		}
		
	}
	
	private static class IbrSupportedTlsNegotiant extends TlsNegotiant {
		
		static {
			oxmFactory.register(ProtocolChain.first(Features.PROTOCOL).next(Register.PROTOCOL),
					new CocParserFactory<>(Register.class));
		}

		public IbrSupportedTlsNegotiant(String hostName, String lang, boolean tlsPreferred) {
			super(hostName, lang, tlsPreferred);
		}
		
	}
	
}
