package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public class ChangeAvailabilityStatusEvent extends RoomEvent<ChangeAvailabilityStatus> {

	public ChangeAvailabilityStatusEvent(Stanza source, JabberId roomJid,
			ChangeAvailabilityStatus eventObject) {
		super(source, roomJid, eventObject);
	}

}
