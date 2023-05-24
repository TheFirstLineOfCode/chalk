package com.thefirstlineofcode.chalk.xeps.component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.basalt.oxm.OxmService;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stream.Stream;
import com.thefirstlineofcode.chalk.core.stream.INegotiationContext;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamer;
import com.thefirstlineofcode.chalk.core.stream.negotiants.AbstractStreamNegotiant;
import com.thefirstlineofcode.chalk.network.ConnectionException;

public class HandshakeNegotiant extends AbstractStreamNegotiant {
	private static IOxmFactory oxmFactory = OxmService.createStreamOxmFactory();
	private String component;
	private String secret;
	
	public HandshakeNegotiant(String component, String secret) {
		this.component = component;
		this.secret = secret;
	}

	@Override
	protected void doNegotiate(INegotiationContext context) throws ConnectionException, NegotiationException {
		Stream openStream = (Stream)oxmFactory.parse(readResponse());
		String sid = openStream.getId();
		
		if (sid == null) {
			throw new NegotiationException("Null stream id.", this, null);
		}
		
		String sidAndSecret = sid + secret;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA1");
			byte[] hash = digest.digest(sidAndSecret.getBytes());
			
			String credentials = new BigInteger(1, hash).toString(16);
			context.write(String.format("<handshake>%s</handshake>", credentials));
			
			String response = readResponse();
			
			if ("<handshake/>".equals(response)) {
				context.setAttribute(StandardStreamer.NEGOTIATION_KEY_BINDED_CHAT_ID, JabberId.parse(component));
				return;
			}
			
			 throw new NegotiationException(this, oxmFactory.parse(response));
		} catch (NoSuchAlgorithmException e) {
			throw new NegotiationException("SHA1 algorithm not supported.", this, e);
		}
	}

}
