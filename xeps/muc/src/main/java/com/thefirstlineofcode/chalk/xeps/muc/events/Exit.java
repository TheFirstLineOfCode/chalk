package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xeps.muc.Affiliation;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public class Exit {
	private String nick;
	private JabberId jid;
	private Affiliation affiliation;
	private boolean self;
	private int sessions;
	
	public Exit(String nick, Affiliation affiliation) {
		this.nick = nick;
		this.affiliation = affiliation;
		this.self = false;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public JabberId getJid() {
		return jid;
	}

	public void setJid(JabberId jid) {
		this.jid = jid;
	}

	public Affiliation getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(Affiliation affiliation) {
		this.affiliation = affiliation;
	}

	public boolean isSelf() {
		return self;
	}

	public void setSelf(boolean self) {
		this.self = self;
	}

	public int getSessions() {
		return sessions;
	}

	public void setSessions(int sessions) {
		this.sessions = sessions;
	}
	
}
