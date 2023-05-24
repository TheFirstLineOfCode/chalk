package com.thefirstlineofcode.chalk.core.stream.negotiants;

import java.util.List;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.annotation.AnnotatedParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stream.FeaturesParser;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stream.sasl.MechanismsParser;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stream.tls.StartTlsParser;
import com.thefirstlineofcode.basalt.xmpp.Constants;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Feature;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Features;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Mechanisms;
import com.thefirstlineofcode.basalt.xmpp.core.stream.tls.StartTls;
import com.thefirstlineofcode.chalk.core.stream.INegotiationContext;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamer;
import com.thefirstlineofcode.chalk.network.ConnectionException;

public class InitialStreamNegotiant extends AbstractStreamNegotiant {
	protected static IOxmFactory oxmFactory = OxmService.createStreamOxmFactory();
	
	static {
		oxmFactory.register(ProtocolChain.first(Features.PROTOCOL),
				new AnnotatedParserFactory<>(FeaturesParser.class));
		oxmFactory.register(ProtocolChain.first(Features.PROTOCOL).next(StartTls.PROTOCOL),
				new AnnotatedParserFactory<>(StartTlsParser.class));
		oxmFactory.register(ProtocolChain.first(Features.PROTOCOL).next(Mechanisms.PROTOCOL),
				new AnnotatedParserFactory<>(MechanismsParser.class));
	}
	
	protected String hostName;
	protected String lang;
	
	public InitialStreamNegotiant(String hostName, String lang) {
		this.hostName = hostName;
		this.lang = lang;
	}

	@Override
	protected void doNegotiate(INegotiationContext context) throws ConnectionException, NegotiationException {
		Stream openStream = new Stream();
		openStream.setDefaultNamespace(Constants.C2S_DEFAULT_NAMESPACE);
		openStream.setTo(JabberId.parse(hostName));
		openStream.setLang(lang);
		openStream.setVersion(Constants.SPECIFICATION_VERSION);
		
		context.write(oxmFactory.translate(openStream));
		
		openStream = (Stream)oxmFactory.parse(readResponse());
		
		Object response = oxmFactory.parse(readResponse());
		if (response instanceof Features) {
			List<Feature> features = ((Features)response).getFeatures();
			context.setAttribute(StandardStreamer.NEGOTIATION_KEY_FEATURES, features);
		} else {
			processError((IError)response, context, oxmFactory);
		}

	}

}
