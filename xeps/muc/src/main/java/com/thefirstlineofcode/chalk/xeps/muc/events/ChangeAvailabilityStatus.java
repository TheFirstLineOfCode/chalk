package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;

public class ChangeAvailabilityStatus {
	private String nick;
	private Presence presence;
	private boolean self;
	
	public ChangeAvailabilityStatus(String nick, Presence presence) {
		this.nick = nick;
		this.presence = presence;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public Presence getPresence() {
		return presence;
	}

	public void setPresence(Presence presence) {
		this.presence = presence;
	}

	public boolean isSelf() {
		return self;
	}

	public void setSelf(boolean self) {
		this.self = self;
	}
	
}
