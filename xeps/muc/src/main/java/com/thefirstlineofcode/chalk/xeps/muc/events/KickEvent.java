package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public class KickEvent extends RoomEvent<Kick> {

	public KickEvent(Stanza source, JabberId roomJid, Kick eventObject) {
		super(source, roomJid, eventObject);
	}

}
