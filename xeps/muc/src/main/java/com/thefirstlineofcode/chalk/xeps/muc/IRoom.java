package com.thefirstlineofcode.chalk.xeps.muc;

import com.thefirstlineofcode.basalt.xeps.muc.RoomInfo;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Message;
import com.thefirstlineofcode.basalt.xmpp.im.stanza.Presence;
import com.thefirstlineofcode.chalk.core.ErrorException;

public interface IRoom {
	JabberId getRoomJid();
	String create(String nick) throws ErrorException;
	String create(String nick, IRoomConfigurator configurator) throws ErrorException;
	<T> void configure(IRoomConfigurator configurator) throws ErrorException;
	String enter(String nick) throws ErrorException;
	String enter(String nick, String password) throws ErrorException;
	void exit();
	boolean isEntered();
	RoomInfo getRoomInfo() throws ErrorException;
	String discoReservedNick();
	String getNick();
	void invite(JabberId invitee, String reason) throws ErrorException;
	Occupant[] getOccupants();
	Occupant getOccupant(String nick);
	void setSubject(String subject);
	void send(Message message);
	void send(String nick, Message message);
	void send(Presence presence);
	String changeNick(String newNick) throws ErrorException;
	void kick(String nick) throws ErrorException;
	void kick(String nick, String reason) throws ErrorException;
}
