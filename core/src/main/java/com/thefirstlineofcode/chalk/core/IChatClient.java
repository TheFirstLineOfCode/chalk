package com.thefirstlineofcode.chalk.core;

import java.util.List;
import java.util.Properties;

import com.thefirstlineofcode.basalt.oxm.IOxmFactory;
import com.thefirstlineofcode.chalk.core.stream.IAuthenticationToken;
import com.thefirstlineofcode.chalk.core.stream.INegotiationListener;
import com.thefirstlineofcode.chalk.core.stream.IStream;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.IConnectionListener;

public interface IChatClient {
	public enum State {
		CLOSED,
		CONNECTING,
		CONNECTED
	}
	
	void setStreamConfig(StreamConfig streamConfig);
	StreamConfig getStreamConfig();
	
	void addNegotiationListener(INegotiationListener negotiationListener);
	void removeNegotiationListener(INegotiationListener negotiationListener);
	List<INegotiationListener> getNegotiationListeners();
	
	void addConnectionListener(IConnectionListener connectionListener);
	boolean removeConnectionListener(IConnectionListener connectionListener);
	IConnectionListener[] getConnectionListeners();
	
	void setDefaultErrorHandler(IErrorHandler errorHandler);
	IErrorHandler getDefaultErrorHandler();
	
	void setDefaultExceptionHandler(IExceptionHandler exceptionHandler);
	IExceptionHandler getExceptionHandler();
	
	void connect(IAuthenticationToken authToken) throws ConnectionException, AuthFailureException;
	boolean isConnected();
	void close();
	boolean isClosed();
	
	void register(Class<? extends IPlugin> pluginClass);
	void register(Class<? extends IPlugin> pluginClass, Properties properties);
	void unregister(Class<? extends IPlugin> pluginClass);
	
	<T> T createApi(Class<T> apiType);
	<T> T createApi(Class<T> apiType, Class<?> constructorParamType, Object constuctorParam);
	<T> T createApi(Class<T> apiType, Class<?>[] constructorParamTypes, Object[] constuctorParams);
	
	IOxmFactory getOxmFactory();
	IStream getStream();
	
	IChatServices getChatServices();
	
	State getState();
}
