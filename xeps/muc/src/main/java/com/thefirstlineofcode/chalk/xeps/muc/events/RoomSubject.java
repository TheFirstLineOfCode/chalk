package com.thefirstlineofcode.chalk.xeps.muc.events;


public class RoomSubject {
	private String nick;
	private String subject;
	
	public RoomSubject(String nick, String subject) {
		this.nick = nick;
		this.subject = subject;
	}
	
	public String getNick() {
		return nick;
	}
	
	public void setNick(String nick) {
		this.nick = nick;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
}
