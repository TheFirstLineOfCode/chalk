package com.thefirstlineofcode.chalk.core.stanza;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public interface IStanzaListener {
	void received(Stanza stanza);
}
