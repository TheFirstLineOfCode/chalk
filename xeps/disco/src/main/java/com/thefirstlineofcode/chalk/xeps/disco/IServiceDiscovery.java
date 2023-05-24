package com.thefirstlineofcode.chalk.xeps.disco;

import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.chalk.core.ErrorException;

public interface IServiceDiscovery {
	boolean discoImServer() throws ErrorException;
	boolean discoAccount(JabberId account) throws ErrorException;
	JabberId[] discoAvailableResources(JabberId account) throws ErrorException;
}
