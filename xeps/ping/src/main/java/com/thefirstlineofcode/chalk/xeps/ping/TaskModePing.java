package com.thefirstlineofcode.chalk.xeps.ping;

import com.thefirstlineofcode.basalt.xeps.ping.Ping;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.RemoteServerTimeout;
import com.thefirstlineofcode.chalk.core.ErrorException;
import com.thefirstlineofcode.chalk.core.IChatServices;
import com.thefirstlineofcode.chalk.core.ISyncTask;
import com.thefirstlineofcode.chalk.core.IUnidirectionalStream;

public class TaskModePing implements IPing {
	private IChatServices chatServices;
	private int timeout;

	public TaskModePing(IChatServices chatServices, int timeout) {
		this.chatServices = chatServices;
		this.timeout = timeout;
	}

	@Override
	public Result ping() {
		try {
			return chatServices.getTaskService().execute(new ISyncTask<Iq, IPing.Result>() {

				@Override
				public void trigger(IUnidirectionalStream<Iq> stream) {
					stream.send(new Iq(Iq.Type.GET, new Ping()), timeout);
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

