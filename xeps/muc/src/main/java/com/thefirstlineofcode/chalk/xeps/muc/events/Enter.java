package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xeps.muc.Affiliation;
import com.thefirstlineofcode.basalt.xeps.muc.Role;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public class Enter {
	private String nick;
	private JabberId jid;
	private Affiliation affiliation;
	private Role role;
	private boolean self;
	private int sessions;
	
	public Enter(String nick, Affiliation affiliation, Role role) {
		this.nick = nick;
		this.affiliation = affiliation;
		this.role = role;
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
	
	public Role getRole() {
		return role;
	}
	
	public void setRole(Role role) {
		this.role = role;
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
