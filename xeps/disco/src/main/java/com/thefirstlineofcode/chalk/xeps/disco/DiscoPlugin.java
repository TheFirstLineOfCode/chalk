package com.thefirstlineofcode.chalk.xeps.disco;

import java.util.Properties;

import com.thefirstlineofcode.basalt.oxm.coc.CocParserFactory;
import com.thefirstlineofcode.basalt.oxm.coc.CocTranslatorFactory;
import com.thefirstlineofcode.basalt.xeps.disco.DiscoInfo;
import com.thefirstlineofcode.basalt.xeps.disco.DiscoItems;
import com.thefirstlineofcode.basalt.xeps.rsm.Set;
import com.thefirstlineofcode.basalt.xeps.xdata.XData;
import com.thefirstlineofcode.basalt.xmpp.core.IqProtocolChain;
import com.thefirstlineofcode.chalk.core.IChatSystem;
import com.thefirstlineofcode.chalk.core.IPlugin;
import com.thefirstlineofcode.chalk.xeps.rsm.RsmPlugin;

public class DiscoPlugin implements IPlugin {

	@Override
	public void init(IChatSystem chatSystem, Properties properties) {
		chatSystem.register(RsmPlugin.class);
		
		chatSystem.registerParser(
				new IqProtocolChain(DiscoInfo.PROTOCOL),
				new CocParserFactory<>(DiscoInfo.class)
		);
		
		chatSystem.registerParser(
				new IqProtocolChain().
				next(DiscoInfo.PROTOCOL).
				next(XData.PROTOCOL),
				new CocParserFactory<>(XData.class)
		);
		
		chatSystem.registerParser(
				new IqProtocolChain(DiscoItems.PROTOCOL),
				new CocParserFactory<>(DiscoItems.class)
		);
		
		chatSystem.registerParser(
				new IqProtocolChain().
					next(DiscoItems.PROTOCOL).
					next(Set.PROTOCOL),
					new CocParserFactory<>(Set.class)
				);
		
		chatSystem.registerTranslator(
				DiscoInfo.class,
				new CocTranslatorFactory<>(DiscoInfo.class)
		);
		
		chatSystem.registerTranslator(
				DiscoItems.class,
				new CocTranslatorFactory<>(DiscoItems.class)
		);
		
		chatSystem.registerApi(IServiceDiscovery.class, ServiceDiscovery.class);
	}

	@Override
	public void destroy(IChatSystem chatSystem) {
		chatSystem.unregisterApi(IServiceDiscovery.class);
		
		chatSystem.unregisterTranslator(DiscoItems.class);
		chatSystem.unregisterTranslator(DiscoInfo.class);
		
		chatSystem.unregisterParser(
				new IqProtocolChain().
				next(DiscoItems.PROTOCOL).
				next(Set.PROTOCOL)
		);
		chatSystem.unregisterParser(
				new IqProtocolChain().
				next(DiscoItems.PROTOCOL).
				next(XData.PROTOCOL)
		);
		chatSystem.unregisterParser(
				new IqProtocolChain(DiscoItems.PROTOCOL)
		);
		chatSystem.unregisterParser(
				new IqProtocolChain(DiscoInfo.PROTOCOL)
		);
		
		chatSystem.unregister(RsmPlugin.class);
	}

}
