package com.thefirstlineofcode.chalk.im.roster;

import com.thefirstlineofcode.basalt.xmpp.im.roster.Roster;

public interface IRosterListener {
	void retrieved(Roster roster);
	void occurred(RosterError error);
	void updated(Roster roster);
}
