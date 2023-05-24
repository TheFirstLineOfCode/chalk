package com.thefirstlineofcode.chalk.xeps.ping;

import java.util.Properties;

import com.thefirstlineofcode.basalt.oxm.parsers.SimpleObjectParserFactory;
import com.thefirstlineofcode.basalt.oxm.translators.SimpleObjectTranslatorFactory;
import com.thefirstlineofcode.basalt.xeps.ping.Ping;
import com.thefirstlineofcode.basalt.xmpp.core.IqProtocolChain;
import com.thefirstlineofcode.chalk.core.IChatSystem;
import com.thefirstlineofcode.chalk.core.IPlugin;

public class PingPlugin implements IPlugin {
	@Override
	public void init(IChatSystem chatSystem, Properties properties) {
		chatSystem.registerParser(
				new IqProtocolChain(Ping.PROTOCOL),
				new SimpleObjectParserFactory<>(Ping.PROTOCOL, Ping.class));
		chatSystem.registerTranslator(
				Ping.class,
				new SimpleObjectTranslatorFactory<>(Ping.class, Ping.PROTOCOL));
		
		chatSystem.registerApi(IPing.class, PingImpl.class, properties);			
	}

	@Override
	public void destroy(IChatSystem chatSystem) {
		chatSystem.unregisterApi(IPing.class);
		chatSystem.unregisterTranslator(Ping.class);
		chatSystem.unregisterParser(new IqProtocolChain(Ping.PROTOCOL));
	}

}
