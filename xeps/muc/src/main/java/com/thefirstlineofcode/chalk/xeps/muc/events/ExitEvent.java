package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public class ExitEvent extends RoomEvent<Exit> {

	public ExitEvent(Stanza source, JabberId roomJid, Exit exit) {
		super(source, roomJid, exit);
	}

}
