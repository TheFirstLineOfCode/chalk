package com.thefirstlineofcode.chalk.xeps.ping;

import com.thefirstlineofcode.chalk.core.IChatServices;

public class PingImpl implements IPing {
	private static final int DEFAULT_TIMEOUT = 2 * 1000;
	
	private IChatServices chatServices;

	private int timeout = DEFAULT_TIMEOUT;
	private boolean taskMode = true;
	
	private IPing delegate;
	
	@Override
	public Result ping() {
		if (taskMode) {
			delegate = new TaskModePing(chatServices, timeout);
		} else {
			delegate = new LegacyModePing(chatServices, timeout);
		}
		
		Result result = delegate.ping();
		delegate = null;
		
		return result;
	}
	
	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	public int getTimeout() {
		return timeout;
	}
	
	public void setTaskMode(boolean taskMode) {
		this.taskMode = taskMode;
	}
	
}
