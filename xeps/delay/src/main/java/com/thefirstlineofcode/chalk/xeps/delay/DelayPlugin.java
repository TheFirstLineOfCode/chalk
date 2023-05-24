package com.thefirstlineofcode.chalk.xeps.delay;

import java.util.Properties;

import com.thefirstlineofcode.basalt.oxm.coc.CocParserFactory;
import com.thefirstlineofcode.basalt.oxm.coc.CocTranslatorFactory;
import com.thefirstlineofcode.basalt.xeps.delay.Delay;
import com.thefirstlineofcode.basalt.xmpp.core.MessageProtocolChain;
import com.thefirstlineofcode.chalk.core.IChatSystem;
import com.thefirstlineofcode.chalk.core.IPlugin;

public class DelayPlugin implements IPlugin {

	@Override
	public void init(IChatSystem chatSystem, Properties properties) {
		chatSystem.registerParser(
				new MessageProtocolChain(Delay.PROTOCOL),
				new CocParserFactory<>(Delay.class)
		);
		
		chatSystem.registerTranslator(
				Delay.class,
				new CocTranslatorFactory<>(Delay.class)
		);
	}

	@Override
	public void destroy(IChatSystem chatSystem) {
		chatSystem.unregisterTranslator(Delay.class);
		
		chatSystem.unregisterParser(
				new MessageProtocolChain(Delay.PROTOCOL)
		);
	}

}
