package com.thefirstlineofcode.chalk.core.stream.negotiants;

import java.util.List;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.translators.SimpleObjectTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Feature;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Session;
import com.thefirstlineofcode.chalk.core.stream.INegotiationContext;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamer;
import com.thefirstlineofcode.chalk.network.ConnectionException;

public class SessionEstablishmentNegotiant extends AbstractStreamNegotiant {
	private static IOxmFactory oxmFactory = OxmService.createMinimumOxmFactory();
	
	static {
		oxmFactory.register(Session.class,
				new SimpleObjectTranslatorFactory<>(
						Session.class,
						Session.PROTOCOL
				)
		);
	}
	
	@Override
	protected void doNegotiate(INegotiationContext context) throws ConnectionException, NegotiationException {
		@SuppressWarnings("unchecked")
		List<Feature> features = (List<Feature>)context.getAttribute(
				StandardStreamer.NEGOTIATION_KEY_FEATURES);
		Session session = findSession(features);
		
		if (session != null) {
			negotiateSession(context);
		}
		
		
	}

	private void negotiateSession(INegotiationContext context) throws ConnectionException, NegotiationException {
		Iq iq = new Iq(Iq.Type.SET);
		iq.setObject(new Session());
		
		context.write(oxmFactory.translate(iq));
		
		Object response = oxmFactory.parse(readResponse());
		
		if (response instanceof Iq) {
			iq = (Iq)response;
			if (Iq.Type.RESULT != iq.getType()) {
				throw new NegotiationException(this);
			}
		} else {
			processError((IError)response, context, oxmFactory);
		}
	}

	private Session findSession(List<Feature> features) {
		for (Feature feature : features) {
			if (feature instanceof Session)
				return (Session)feature;
		}
		
		return null;
	}

}
