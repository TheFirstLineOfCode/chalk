package com.thefirstlineofcode.chalk.xeps.muc.events;

import com.thefirstlineofcode.basalt.xeps.muc.Affiliation;
import com.thefirstlineofcode.basalt.xeps.muc.Role;

public class Kicked extends Kick {

	public Kicked(String nick, Affiliation affiliation, Role role) {
		super(nick, affiliation, role);
	}

}
