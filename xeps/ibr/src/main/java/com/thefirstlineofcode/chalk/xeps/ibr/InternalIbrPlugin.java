package com.thefirstlineofcode.chalk.xeps.ibr;

import java.util.Properties;

import com.thefirstlineofcode.basalt.oxm.coc.CocParserFactory;
import com.thefirstlineofcode.basalt.xeps.ibr.IqRegister;
import com.thefirstlineofcode.basalt.xeps.ibr.oxm.IqRegisterParserFactory;
import com.thefirstlineofcode.basalt.xeps.ibr.oxm.IqRegisterTranslatorFactory;
import com.thefirstlineofcode.basalt.xeps.oob.XOob;
import com.thefirstlineofcode.basalt.xeps.xdata.XData;
import com.thefirstlineofcode.basalt.xmpp.core.IqProtocolChain;
import com.thefirstlineofcode.chalk.core.IChatSystem;
import com.thefirstlineofcode.chalk.core.IPlugin;
import com.thefirstlineofcode.chalk.xeps.oob.OobPlugin;
import com.thefirstlineofcode.chalk.xeps.xdata.XDataPlugin;

public class InternalIbrPlugin implements IPlugin {
	@Override
	public void init(IChatSystem chatSystem, Properties properties) {
		chatSystem.register(OobPlugin.class);
		chatSystem.register(XDataPlugin.class);
		
		chatSystem.registerParser(
				new IqProtocolChain(IqRegister.PROTOCOL),
				new IqRegisterParserFactory()
		);
		
		chatSystem.registerParser(
				new IqProtocolChain().
				next(IqRegister.PROTOCOL).
				next(XData.PROTOCOL),
				new CocParserFactory<>(XData.class)
		);
		
		chatSystem.registerParser(
				new IqProtocolChain().
				next(IqRegister.PROTOCOL).
				next(XOob.PROTOCOL),
				new CocParserFactory<>(XOob.class)
		);
		
		chatSystem.registerTranslator(
				IqRegister.class,
				new IqRegisterTranslatorFactory()
		);
	}

	@Override
	public void destroy(IChatSystem chatSystem) {
		chatSystem.unregisterTranslator(IqRegister.class);
		
		chatSystem.unregisterParser(
				new IqProtocolChain(IqRegister.PROTOCOL)
		);
		
		chatSystem.unregisterParser(
				new IqProtocolChain().
				next(IqRegister.PROTOCOL).
				next(XData.PROTOCOL)
		);
		
		chatSystem.unregisterParser(
				new IqProtocolChain().
				next(IqRegister.PROTOCOL).
				next(XOob.PROTOCOL)
		);
		
		chatSystem.unregister(XDataPlugin.class);
		chatSystem.unregister(OobPlugin.class);
	}
}
