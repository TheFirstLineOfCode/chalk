package com.thefirstlineofcode.chalk.core.stream;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public interface IStanzaWatcher {
	void sent(Stanza stanza, String message);
	void received(Stanza stanza, String message);
}
