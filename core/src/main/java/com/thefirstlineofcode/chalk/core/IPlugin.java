package com.thefirstlineofcode.chalk.core;

import java.util.Properties;

public interface IPlugin {
	void init(IChatSystem chatSystem, Properties properties);
	void destroy(IChatSystem chatSystem);
}
