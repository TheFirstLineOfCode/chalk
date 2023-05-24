package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public class Invitation {
	private JabberId invitor;
	private JabberId room;
	private String reason;
	private String password;
	private String thread;
	private Boolean continuee;
	
	public Invitation(JabberId invitor, JabberId room, String reason) {
		this.invitor = invitor;
		this.room = room;
		this.reason = reason;
	}
	
	public JabberId getInvitor() {
		return invitor;
	}
	
	public void setInvitor(JabberId invitor) {
		this.invitor = invitor;
	}
	
	public JabberId getRoom() {
		return room;
	}
	
	public void setRoom(JabberId room) {
		this.room = room;
	}
	
	public String getReason() {
		return reason;
	}
	
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getThread() {
		return thread;
	}
	
	public void setThread(String thread) {
		this.thread = thread;
	}
	
	public Boolean getContinue() {
		return continuee;
	}
	
	public void setContinue(Boolean continuee) {
		this.continuee = continuee;
	}
	
}
