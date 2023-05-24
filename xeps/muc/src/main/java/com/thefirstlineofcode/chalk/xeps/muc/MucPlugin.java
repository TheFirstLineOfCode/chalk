package com.thefirstlineofcode.chalk.xeps.muc;

import java.util.Properties;

import com.thefirstlineofcode.basalt.oxm.coc.CocParserFactory;
import com.thefirstlineofcode.basalt.oxm.coc.CocTranslatorFactory;
import com.thefirstlineofcode.basalt.xeps.muc.Muc;
import com.thefirstlineofcode.basalt.xeps.muc.admin.MucAdmin;
import com.thefirstlineofcode.basalt.xeps.muc.owner.MucOwner;
import com.thefirstlineofcode.basalt.xeps.muc.user.MucUser;
import com.thefirstlineofcode.basalt.xeps.muc.xconference.XConference;
import com.thefirstlineofcode.basalt.xeps.xdata.XData;
import com.thefirstlineofcode.basalt.xmpp.core.IqProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.MessageProtocolChain;
import com.thefirstlineofcode.basalt.xmpp.core.PresenceProtocolChain;
import com.thefirstlineofcode.chalk.core.IChatSystem;
import com.thefirstlineofcode.chalk.core.IPlugin;
import com.thefirstlineofcode.chalk.xeps.address.AddressPlugin;
import com.thefirstlineofcode.chalk.xeps.delay.DelayPlugin;
import com.thefirstlineofcode.chalk.xeps.disco.DiscoPlugin;
import com.thefirstlineofcode.chalk.xeps.xdata.XDataPlugin;

public class MucPlugin implements IPlugin {

	@Override
	public void init(IChatSystem chatSystem, Properties properties) {
		chatSystem.register(DiscoPlugin.class);
		
		chatSystem.register(XDataPlugin.class);
		
		chatSystem.register(DelayPlugin.class);
		
		chatSystem.register(AddressPlugin.class);
		
		chatSystem.registerParser(
				new PresenceProtocolChain(Muc.PROTOCOL),
				new CocParserFactory<>(Muc.class)
		);
		
		chatSystem.registerParser(
				new PresenceProtocolChain(MucUser.PROTOCOL),
				new CocParserFactory<>(MucUser.class)
		);
		
		chatSystem.registerParser(
				new MessageProtocolChain(MucUser.PROTOCOL),
				new CocParserFactory<>(MucUser.class)
		);
		
		chatSystem.registerParser(
				new IqProtocolChain(MucOwner.PROTOCOL),
				new CocParserFactory<>(MucOwner.class)
		);
		
		chatSystem.registerParser(
				new IqProtocolChain().
					next(MucOwner.PROTOCOL).
					next(XData.PROTOCOL),
				new CocParserFactory<>(XData.class)
		);
		
		chatSystem.registerParser(
				new MessageProtocolChain(XConference.PROTOCOL),
				new CocParserFactory<>(XConference.class)
		);
		
		chatSystem.registerTranslator(
				Muc.class,
				new CocTranslatorFactory<>(Muc.class)
		);
		
		chatSystem.registerTranslator(
				MucUser.class,
				new CocTranslatorFactory<>(MucUser.class)
		);
		
		chatSystem.registerTranslator(
				MucAdmin.class,
				new CocTranslatorFactory<>(MucAdmin.class)
		);
		
		chatSystem.registerTranslator(
				MucOwner.class,
				new CocTranslatorFactory<>(MucOwner.class)
		);
		
		chatSystem.registerTranslator(
				XConference.class,
				new CocTranslatorFactory<>(XConference.class)
		);
		
		chatSystem.registerApi(IMucService.class, MucService.class);
	}

	@Override
	public void destroy(IChatSystem chatSystem) {
		chatSystem.unregisterApi(IMucService.class);
		
		chatSystem.unregisterParser(
			new MessageProtocolChain(
					XConference.PROTOCOL));
		
		chatSystem.unregisterParser(
				new IqProtocolChain().
					next(MucOwner.PROTOCOL).
					next(XData.PROTOCOL));
		
		chatSystem.unregisterParser(
				new IqProtocolChain(MucOwner.PROTOCOL));
		
		chatSystem.unregisterParser(
				new MessageProtocolChain(MucUser.PROTOCOL));
		
		chatSystem.unregisterParser(
				new PresenceProtocolChain(MucUser.PROTOCOL));
		
		chatSystem.unregisterParser(
				new PresenceProtocolChain(Muc.PROTOCOL));
		
		chatSystem.unregisterTranslator(XConference.class);
		
		chatSystem.unregisterTranslator(MucOwner.class);
		
		chatSystem.unregisterTranslator(MucUser.class);
		
		chatSystem.unregisterTranslator(Muc.class);
		
		chatSystem.unregister(AddressPlugin.class);
		
		chatSystem.unregister(DelayPlugin.class);
		
		chatSystem.unregister(DiscoPlugin.class);
		
		chatSystem.unregister(XDataPlugin.class);
	}

}
