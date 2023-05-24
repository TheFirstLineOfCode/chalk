package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public class ChangeNickEvent extends RoomEvent<ChangeNick> {

	public ChangeNickEvent(Stanza source, JabberId roomJid, ChangeNick changeNick) {
		super(source, roomJid, changeNick);
	}

}
