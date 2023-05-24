package com.thefirstlineofcode.chalk.core;

import java.util.List;

import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.im.stanza.IPresenceListener;

public interface IPresenceService {
	void send(Presence presence);
	void addListener(IPresenceListener listener);
	void removeListener(IPresenceListener listener);
	List<IPresenceListener> getListeners();
	
	Presence getCurrent();
}
