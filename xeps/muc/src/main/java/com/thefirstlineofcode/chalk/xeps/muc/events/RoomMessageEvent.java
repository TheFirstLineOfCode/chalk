package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public class RoomMessageEvent extends RoomEvent<RoomMessage> {

	public RoomMessageEvent(Stanza source, JabberId roomJid, RoomMessage message) {
		super(source, roomJid, message);
	}

}
