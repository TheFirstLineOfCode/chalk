package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.chalk.core.stream.IStream;

public interface IChatServices {
	IOxmFactory getOxmFactory();
	IStream getStream();
	ITaskService getTaskService();
	IPresenceService getPresenceService();
	IMessageService getMessageService();
	IIqService getIqService();
	IErrorService getErrorService();
	
	public <T> T createApi(Class<T> apiType);
}
