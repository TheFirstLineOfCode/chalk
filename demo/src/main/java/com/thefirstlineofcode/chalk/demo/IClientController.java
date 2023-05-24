package com.thefirstlineofcode.chalk.demo;

public interface IClientController {
	void startClient(Class<? extends Client> askerClass, Class<? extends Client> clientClass);
	void stopClient(Class<? extends Client> askerClass, Class<? extends Client> clientClass);
}
