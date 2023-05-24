package com.thefirstlineofcode.chalk.xeps.muc;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;

public class PublicRoom {
	private JabberId jid;
	private String name;
	
	public PublicRoom(JabberId jid, String name) {
		this.jid = jid;
		this.name = name;
	}
	
	public JabberId getJid() {
		return jid;
	}
	
	public void setJid(JabberId jid) {
		this.jid = jid;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
}
