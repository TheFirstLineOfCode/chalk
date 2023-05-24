package com.thefirstlineofcode.chalk.xeps.address;

import java.util.Properties;

import com.thefirstlineofcode.basalt.oxm.coc.CocParserFactory;
import com.thefirstlineofcode.basalt.oxm.coc.CocTranslatorFactory;
import com.thefirstlineofcode.basalt.xeps.address.Addresses;
import com.thefirstlineofcode.basalt.xmpp.core.MessageProtocolChain;
import com.thefirstlineofcode.chalk.core.IChatSystem;
import com.thefirstlineofcode.chalk.core.IPlugin;

public class AddressPlugin implements IPlugin {

	@Override
	public void init(IChatSystem chatSystem, Properties properties) {
		chatSystem.registerParser(
				new MessageProtocolChain(Addresses.PROTOCOL),
				new CocParserFactory<>(Addresses.class)
		);
		
		chatSystem.registerTranslator(
				Addresses.class,
				new CocTranslatorFactory<>(Addresses.class)
		);
	}

	@Override
	public void destroy(IChatSystem chatSystem) {
		chatSystem.unregisterTranslator(Addresses.class);
		
		chatSystem.unregisterParser(
				new MessageProtocolChain(Addresses.PROTOCOL)
		);
	}

}
