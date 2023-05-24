package com.thefirstlineofcode.chalk.xeps.component;

import java.util.ArrayList;
import java.util.List;

import com.thefirstlineofcode.chalk.core.stream.AbstractStreamer;
import com.thefirstlineofcode.chalk.core.stream.IStreamNegotiant;
import com.thefirstlineofcode.chalk.network.IConnection;

public class ComponentStreamer extends AbstractStreamer {
	private String componentName;
	
	public ComponentStreamer(ComponentStreamConfig streamConfig) {
		this(streamConfig, null);
	}
	
	public ComponentStreamer(ComponentStreamConfig streamConfig, IConnection connection) {
		super(streamConfig, connection);
		componentName = ((ComponentStreamConfig)streamConfig).getComponentName();
	}
	
	@Override
	protected List<IStreamNegotiant> createNegotiants() {
		List<IStreamNegotiant> negotiants = new ArrayList<>();
		
		InitialStreamNegotiant initialStreamNegotiant = createInitialStreamNegotiant();
		negotiants.add(initialStreamNegotiant);
		
		HandshakeNegotiant handshakeNegotiant = createHandshakeNegotiant();
		negotiants.add(handshakeNegotiant);
		
		setNegotiationReadResponseTimeout(negotiants);
		
		return negotiants;
	}
	
	private HandshakeNegotiant createHandshakeNegotiant() {
		String secret = (String)((SecretToken)authToken).getCredentials();
		
		return new HandshakeNegotiant(componentName, secret);
	}

	protected InitialStreamNegotiant createInitialStreamNegotiant() {
		return new InitialStreamNegotiant(componentName, streamConfig.getLang());
	}

}
