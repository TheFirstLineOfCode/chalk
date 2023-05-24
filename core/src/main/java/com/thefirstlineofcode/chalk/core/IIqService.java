package com.thefirstlineofcode.chalk.core;

import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.core.Protocol;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.chalk.core.stanza.IIqListener;

public interface IIqService {
	void send(Iq iq);
	void addListener(IIqListener listener);
	void removeListener(IIqListener listener);
	void addListener(Protocol protocol, IIqListener listener);
	void removeListener(Protocol protocol);
	
	IIqListener getListener(Protocol protocol);
	List<IIqListener> getListeners();
}
