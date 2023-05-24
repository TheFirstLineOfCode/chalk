package com.thefirstlineofcode.chalk.xeps.ping;

import com.thefirstlineofcode.basalt.xeps.ping.Ping;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.RemoteServerTimeout;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.StanzaError;
import com.thefirstlineofcode.chalk.core.ErrorException;
import com.thefirstlineofcode.chalk.core.IChatServices;
import com.thefirstlineofcode.chalk.core.ISyncIqOperation;
import com.thefirstlineofcode.chalk.core.IUnidirectionalStream;
import com.thefirstlineofcode.chalk.core.SyncOperationTemplate;

public class LegacyModePing implements IPing {
	private IChatServices chatServices;
	private String id;
	private int timeout;
	
	public LegacyModePing(IChatServices chatServices, int timeout) {
		this.chatServices = chatServices;
		this.timeout = timeout;
	}

	@Override
	public IPing.Result ping() {
		SyncOperationTemplate<Iq, IPing.Result> template = new SyncOperationTemplate<>(chatServices);
		
		try {
			return template.execute(new ISyncIqOperation<IPing.Result>() {

				@Override
				public void trigger(IUnidirectionalStream<Iq> stream) {
					Iq iq = new Iq(Iq.Type.GET, new Ping());
					id = iq.getId();
					
					stream.send(iq, timeout);
				}

				@Override
				public boolean isErrorOccurred(StanzaError error) {
					if (id.equals(error.getId()))
						return true;
					
					return false;
				}

				@Override
				public boolean isResultReceived(Iq iq) {
					if (id.equals(iq.getId()))
						return true;
					
					return false;
				}

				@Override
				public Result processResult(Iq iq) {
					return IPing.Result.PONG;
				}
			});
		} catch (ErrorException e) {
			if (e.getError().getDefinedCondition().equals(RemoteServerTimeout.DEFINED_CONDITION)) {
				return IPing.Result.TIME_OUT;
			} else {
				return IPing.Result.SERVICE_UNAVAILABLE;
			}
		}
	}

	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	public int getTimeout() {
		return timeout;
	}

}
