package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;

public class RoomEvent<T> {
	protected Stanza source;
	protected JabberId roomJid;
	protected T eventObject;
	
	public RoomEvent(Stanza source, JabberId roomJid, T eventObject) {
		this.source = source;
		this.roomJid = roomJid;
		this.eventObject = eventObject;
	}
	
	public JabberId getRoomJid() {
		return roomJid;
	}
	
	public T getEventObject() {
		return eventObject;
	}

	public void setRoomJid(JabberId roomJid) {
		this.roomJid = roomJid;
	}

	public void setEventObject(T eventObject) {
		this.eventObject = eventObject;
	}

	public Stanza getSource() {
		return source;
	}

	public void setSource(Stanza source) {
		this.source = source;
	}
	
}
