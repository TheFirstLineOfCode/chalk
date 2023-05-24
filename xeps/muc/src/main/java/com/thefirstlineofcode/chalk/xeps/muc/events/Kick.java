package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xeps.muc.Affiliation;
import com.thefirstlineofcode.basalt.xeps.muc.Role;
import com.thefirstlineofcode.basalt.xeps.muc.user.Actor;

public class Kick {
	private String nick;
	private Affiliation affilation;
	private Role role;
	private Actor actor;
	private String reason;
	
	public Kick(String nick, Affiliation affiliation, Role role) {
		this.nick = nick;
		this.affilation = affiliation;
		this.role = role;
	}
	
	public String getNick() {
		return nick;
	}
	
	public void setNick(String nick) {
		this.nick = nick;
	}

	public Affiliation getAffilation() {
		return affilation;
	}

	public void setAffilation(Affiliation affilation) {
		this.affilation = affilation;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
	
	public Actor getActor() {
		return actor;
	}
	
	public void setActor(Actor actor) {
		this.actor = actor;
	}
	
	public String getReason() {
		return reason;
	}
	
	public void setReason(String reason) {
		this.reason = reason;
	}
	
}
