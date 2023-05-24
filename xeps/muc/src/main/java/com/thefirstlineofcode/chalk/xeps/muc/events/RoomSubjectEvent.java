package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public class RoomSubjectEvent extends RoomEvent<RoomSubject> {

	public RoomSubjectEvent(Stanza source, JabberId roomJid, RoomSubject subject) {
		super(source, roomJid, subject);
	}

}
