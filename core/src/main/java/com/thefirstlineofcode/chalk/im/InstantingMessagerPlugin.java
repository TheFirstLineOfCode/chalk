package com.thefirstlineofcode.chalk.im;

import java.util.Properties;

import com.thefirstlineofcode.basalt.oxm.annotation.AnnotatedParserFactory;
import com.thefirstlineofcode.basalt.xmpp.core.ProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.im.roster.Roster;
import com.thefirstlineofcode.basalt.xmpp.im.roster.RosterParser;
import com.thefirstlineofcode.basalt.xmpp.im.roster.RosterTranslatorFactory;
import com.thefirstlineofcode.chalk.core.IChatSystem;
import com.thefirstlineofcode.chalk.core.IPlugin;

public class InstantingMessagerPlugin implements IPlugin {

	@Override
	public void init(IChatSystem chatSystem, Properties properties) {
		chatSystem.registerParser(
				ProtocolChain.first(Iq.PROTOCOL).
				next(Roster.PROTOCOL),
				new AnnotatedParserFactory<>(RosterParser.class)
		);
		chatSystem.registerTranslator(
				Roster.class,
				new RosterTranslatorFactory()
		);
		chatSystem.registerApi(IInstantingMessager.class, InstantingMessager.class);
	}

	@Override
	public void destroy(IChatSystem chatSystem) {
		chatSystem.unregisterApi(IInstantingMessager.class);
		chatSystem.unregisterTranslator(Roster.class);
		chatSystem.unregisterParser(
				ProtocolChain.first(Iq.PROTOCOL).
				next(Roster.PROTOCOL));
	}

}
