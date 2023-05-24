package com.thefirstlineofcode.chalk.im.stanza;

import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;

public interface IPresenceListener {
	void received(Presence presence);
}
