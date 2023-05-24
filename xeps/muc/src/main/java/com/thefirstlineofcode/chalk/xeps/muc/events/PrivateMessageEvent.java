package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public class PrivateMessageEvent extends RoomEvent<RoomMessage> {

	public PrivateMessageEvent(Stanza source, JabberId roomJid, RoomMessage message) {
		super(source, roomJid, message);
	}
	
}
