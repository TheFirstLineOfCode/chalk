package com.thefirstlineofcode.chalk.xeps.xdata;

import java.util.Properties;

import com.thefirstlineofcode.basalt.oxm.coc.CocTranslatorFactory;
import com.thefirstlineofcode.basalt.xeps.xdata.XData;
import com.thefirstlineofcode.chalk.core.IChatSystem;
import com.thefirstlineofcode.chalk.core.IPlugin;

public class XDataPlugin implements IPlugin {

	@Override
	public void init(IChatSystem chatSystem, Properties properties) {
		chatSystem.registerTranslator(XData.class,
				new CocTranslatorFactory<>(
						XData.class
				)
		);
	}

	@Override
	public void destroy(IChatSystem chatSystem) {
		chatSystem.unregisterTranslator(XData.class);
	}

}
