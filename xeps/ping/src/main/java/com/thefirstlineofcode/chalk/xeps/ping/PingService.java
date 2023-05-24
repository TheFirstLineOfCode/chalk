package com.thefirstlineofcode.chalk.xeps.ping;

import com.thefirstlineofcode.basalt.xeps.ping.Ping;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolException;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.ServiceUnavailable;
import com.thefirstlineofcode.chalk.core.IChatServices;
import com.thefirstlineofcode.chalk.core.stanza.IIqListener;
import com.thefirstlineofcode.chalk.core.stream.IStream;

public class PingService implements IPingService, IIqListener {
	private IChatServices chatServices;
	private boolean enabled;
	private boolean clientPingSupported;
	private JabberId host;
	
	public PingService(IChatServices chatServices) {
		this.chatServices = chatServices;
		
		IStream stream = chatServices.getStream();
		if (stream == null || stream.isClosed())
			throw new IllegalStateException("Null stream or stream is closed.");
		host = JabberId.parse(stream.getStreamConfig().getHost());
		
		enabled = false;
		clientPingSupported = false;
		
		chatServices.getIqService().addListener(Ping.PROTOCOL, this);
	}

	@Override
	public void enable(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void supportClientPing(boolean clientPingSupported) {
		this.clientPingSupported = clientPingSupported;
	}

	@Override
	public boolean isClientPingSupported() {
		return clientPingSupported;
	}

	@Override
	public void received(Iq iq) {
		if (!enabled)
			throw new ProtocolException(new ServiceUnavailable());
		
		if (iq.getFrom() != null && !iq.getFrom().equals(host) && !clientPingSupported)
			throw new ProtocolException(new ServiceUnavailable());
		
		chatServices.getIqService().send(Iq.createResult(iq, new Ping()));
	}

}
