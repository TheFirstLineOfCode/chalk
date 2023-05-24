package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public class EnterEvent extends RoomEvent<Enter> {

	public EnterEvent(Stanza source, JabberId roomJid, Enter enter) {
		super(source, roomJid, enter);
	}

}
