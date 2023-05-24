package com.thefirstlineofcode.chalk.xeps.muc.events;


public class RoomMessage {
	private String nick;
	private String message;
	
	public RoomMessage(String nick, String message) {
		this.nick = nick;
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getNick() {
		return nick;
	}
	
	public void setNick(String nick) {
		this.nick = nick;
	}
	
}
