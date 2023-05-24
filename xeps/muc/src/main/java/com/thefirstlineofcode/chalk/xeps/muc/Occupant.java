package com.thefirstlineofcode.chalk.xeps.muc;

import com.thefirstlineofcode.basalt.xeps.muc.Affiliation;
import com.thefirstlineofcode.basalt.xeps.muc.Role;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public class Occupant {
	private JabberId jid;
	private String nick;
	private Affiliation affiliation;
	private Role role;
	private int sessions;
	
	public Occupant(String nick, Affiliation affiliation, Role role) {
		this.nick = nick;
		this.affiliation = affiliation;
		this.role = role;
		this.sessions = 0;
	}
	
	public JabberId getJid() {
		return jid;
	}
	
	public void setJid(JabberId jid) {
		this.jid = jid;
	}
	
	public String getNick() {
		return nick;
	}
	
	public void setNick(String nick) {
		this.nick = nick;
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
	
	public void addSession() {
		sessions++;
	}
	
	public void removeSession() {
		sessions--;
	}
	
	public int getSessions() {
		return sessions;
	}
}
