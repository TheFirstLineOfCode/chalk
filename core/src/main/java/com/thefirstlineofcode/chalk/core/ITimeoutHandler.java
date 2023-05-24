package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public interface ITimeoutHandler {
	void process(Stanza stanza);
}
