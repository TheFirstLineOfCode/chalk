package com.thefirstlineofcode.chalk.core.stream.negotiants;

import java.util.List;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.oxm.annotation.AnnotatedParserFactory;
import com.thefirstlineofcode.basalt.oxm.parsers.core.stream.BindParser;
import com.thefirstlineofcode.basalt.oxm.translators.core.stream.BindTranslatorFactory;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Bind;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Feature;
import com.thefirstlineofcode.chalk.core.stream.INegotiationContext;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamer;
import com.thefirstlineofcode.chalk.network.ConnectionException;

public class ResourceBindingNegotiant extends AbstractStreamNegotiant {
	private static IOxmFactory oxmFactory = OxmService.createMinimumOxmFactory();
	
	private String resource;
	
	static {
		oxmFactory.register(ProtocolChain.first(Iq.PROTOCOL).next(Bind.PROTOCOL),
					new AnnotatedParserFactory<>(BindParser.class)
				);
		
		oxmFactory.register(Bind.class, new BindTranslatorFactory());
	}
	
	public ResourceBindingNegotiant(String resource) {
		this.resource = resource;
	}

	@Override
	protected void doNegotiate(INegotiationContext context) throws ConnectionException, NegotiationException {
		@SuppressWarnings("unchecked")
		List<Feature> features = (List<Feature>)context.getAttribute(
				StandardStreamer.NEGOTIATION_KEY_FEATURES);
		Bind bind = findBind(features);
		
		if (bind != null) {
			negotiateBind(context);
		}
	}

	private void negotiateBind(INegotiationContext context) throws ConnectionException, NegotiationException {
		Iq iq = new Iq(Iq.Type.SET);
		iq.setObject(new Bind(resource));
		
		context.write(oxmFactory.translate(iq));
		
		Object response = oxmFactory.parse(readResponse());
		
		if (response instanceof Iq) {
			iq = (Iq)response;
			
			if (iq.getObject() instanceof Bind) {
				Bind bind = iq.getObject();
				
				if (bind.getJid() != null) {
					context.setAttribute(StandardStreamer.NEGOTIATION_KEY_BINDED_CHAT_ID, bind.getJid());
				}
			}
			
			if (context.getAttribute(StandardStreamer.NEGOTIATION_KEY_BINDED_CHAT_ID) == null) {
				throw new NegotiationException(this);
			}
		} else {
			processError((IError)response, context, oxmFactory);
		}
	}

	private Bind findBind(List<Feature> features) {
		for (Feature feature : features) {
			if (feature instanceof Bind)
				return (Bind)feature;
		}
		
		return null;
	}

}
