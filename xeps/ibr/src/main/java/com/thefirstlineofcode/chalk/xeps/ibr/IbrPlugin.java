package com.thefirstlineofcode.chalk.xeps.ibr;

import java.util.Properties;

import com.thefirstlineofcode.chalk.core.IChatSystem;
import com.thefirstlineofcode.chalk.core.IPlugin;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;

public class IbrPlugin implements IPlugin {

	@Override
	public void init(IChatSystem chatSystem, Properties properties) {
		StreamConfig streamConfig = chatSystem.getStreamConfig();
		
		if (!(streamConfig instanceof StandardStreamConfig)) {
			throw new IllegalArgumentException(String.format("IBR plugin needs a StandardStreamConfig."));
		}
		
		Properties apiProperties = new Properties();
		apiProperties.put("streamConfig", streamConfig);
		
		chatSystem.registerApi(IRegistration.class, Registration.class, apiProperties, false);
	}

	@Override
	public void destroy(IChatSystem chatSystem) {
		chatSystem.unregisterApi(IRegistration.class);
	}

}
