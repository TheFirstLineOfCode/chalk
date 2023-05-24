package com.thefirstlineofcode.chalk.xeps.ibr;

import java.util.List;

import com.thefirstlineofcode.basalt.xeps.ibr.Register;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Feature;
import com.thefirstlineofcode.chalk.core.stream.AbstractStreamer;
import com.thefirstlineofcode.chalk.core.stream.INegotiationContext;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.negotiants.AbstractStreamNegotiant;
import com.thefirstlineofcode.chalk.network.ConnectionException;

public class IbrNegotiant extends AbstractStreamNegotiant {
	
	
	@Override
	protected void doNegotiate(INegotiationContext context) throws ConnectionException, NegotiationException {
		@SuppressWarnings("unchecked")
		List<Feature> features = (List<Feature>)context.getAttribute(AbstractStreamer.NEGOTIATION_KEY_FEATURES);
		
		for (Feature feature : features) {
			if (feature instanceof Register) {
				return;
			}
		}
		
		throw new NegotiationException(this, IbrError.NOT_SUPPORTED);
	}

}
