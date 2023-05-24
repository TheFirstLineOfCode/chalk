package com.thefirstlineofcode.chalk.core.stanza;

import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;

public interface IIqListener {
	void received(Iq iq);
}
