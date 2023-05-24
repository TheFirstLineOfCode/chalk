package com.thefirstlineofcode.chalk.xeps.ibr;

import java.util.ArrayList;
import java.util.List;

import com.thefirstlineofcode.basalt.xeps.ibr.IqRegister;
import com.thefirstlineofcode.basalt.xeps.ibr.RegistrationForm;
import com.thefirstlineofcode.basalt.xeps.xdata.XData;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Iq;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.Stanza;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.Conflict;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.NotAcceptable;
import com.thefirstlineofcode.basalt.xmpp.core.stanza.error.RemoteServerTimeout;
import com.thefirstlineofcode.chalk.core.AuthFailureException;
import com.thefirstlineofcode.chalk.core.ErrorException;
import com.thefirstlineofcode.chalk.core.IChatClient;
import com.thefirstlineofcode.chalk.core.ISyncTask;
import com.thefirstlineofcode.chalk.core.IUnidirectionalStream;
import com.thefirstlineofcode.chalk.core.stream.INegotiationListener;
import com.thefirstlineofcode.chalk.core.stream.IStream;
import com.thefirstlineofcode.chalk.core.stream.IStreamNegotiant;
import com.thefirstlineofcode.chalk.core.stream.NegotiationException;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.IConnectionListener;

public class Registration implements IRegistration, INegotiationListener {
	private StandardStreamConfig streamConfig;
	private List<IConnectionListener> connectionListeners = new ArrayList<>();
	private List<INegotiationListener> negotiationListeners = new ArrayList<>();
	
	@Override
	public void register(IRegistrationCallback callback) throws RegistrationException {
		IbrChatClient ibrChatClient = new IbrChatClient(streamConfig);
		ibrChatClient.register(InternalIbrPlugin.class);
		
		for (IConnectionListener connectionListener : connectionListeners) {
			ibrChatClient.addConnectionListener(connectionListener);
		}
		
		for (INegotiationListener negotiationListener : negotiationListeners) {
			ibrChatClient.addNegotiationListener(negotiationListener);
		}
		
		ibrChatClient.addNegotiationListener(this);
		
		try {
			ibrChatClient.connect(null);
		} catch (ConnectionException e) {
			if (!ibrChatClient.isClosed())
				ibrChatClient.close();
			
			throw new RegistrationException(IbrError.CONNECTION_ERROR, e);
		} catch (AuthFailureException e) {
			// It's impossible
		}
		
		
		try {
			Object filled = callback.fillOut(getRegistrationForm(ibrChatClient));
			ibrChatClient.getChatServices().getTaskService().execute(new RegisterTask(filled));
		} catch (ErrorException e) {
			IError error = e.getError();
			if (error.getDefinedCondition().equals(RemoteServerTimeout.DEFINED_CONDITION)) {
				throw new RegistrationException(IbrError.TIMEOUT);
			} else if (error.getDefinedCondition().equals(Conflict.DEFINED_CONDITION)) {
				throw new RegistrationException(IbrError.CONFLICT);
			} else if (error.getDefinedCondition().equals(NotAcceptable.DEFINED_CONDITION)) {
				throw new RegistrationException(IbrError.NOT_ACCEPTABLE);
			} else {
				throw new RegistrationException(IbrError.UNKNOWN, e);
			}
		} finally {
			if (!ibrChatClient.isClosed())
				ibrChatClient.close();
		}
	}
	
	private class RegisterTask implements ISyncTask<Iq, Void>  {
		private IqRegister iqRegister;
		
		public RegisterTask(Object filled) {
			iqRegister = new IqRegister();
			
			if (filled instanceof RegistrationForm) {
				iqRegister.setRegister(filled);
			} else if (filled instanceof XData) {
				iqRegister.setXData((XData)filled);
			} else {
				throw new IllegalArgumentException("Must be RegistrationForm or XData.");
			}
		}

		@Override
		public void trigger(IUnidirectionalStream<Iq> stream) {
			Iq iq = new Iq(Iq.Type.SET, iqRegister, Stanza.generateId("ibr"));
			iq.setObject(iqRegister);
			
			stream.send(iq);
		}

		@Override
		public Void processResult(Iq iq) {
			return null;
		}
		
	}
	
	private IqRegister getRegistrationForm(IChatClient chatClient) throws ErrorException {
		return chatClient.getChatServices().getTaskService().execute(new ISyncTask<Iq, IqRegister>() {

			@Override
			public void trigger(IUnidirectionalStream<Iq> stream) {
				Iq iq = new Iq(Iq.Type.GET, new IqRegister(), Stanza.generateId("ibr"));
				stream.send(iq);
			}

			@Override
			public IqRegister processResult(Iq iq) {
				return (IqRegister)iq.getObject();
			}
		});
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Feature not implemented.");
	}

	@Override
	public void addConnectionListener(IConnectionListener listener) {
		connectionListeners.add(listener);
	}

	@Override
	public void removeConnectionListener(IConnectionListener listener) {
		connectionListeners.remove(listener);
	}

	@Override
	public void addNegotiationListener(INegotiationListener listener) {
		negotiationListeners.add(listener);
	}

	@Override
	public void removeNegotiationListener(INegotiationListener listener) {
		negotiationListeners.remove(listener);
	}

	@Override
	public void before(IStreamNegotiant source) {
		// Do nothing.
		
	}

	@Override
	public void after(IStreamNegotiant source) {
		// Do nothing.
		
	}
	
	@Override
	public void occurred(NegotiationException exception) {
		// Do nothing.
		
	}
	
	@Override
	public void done(IStream stream) {
		if (stream.getKeepAliveManager() != null &&
				stream.getKeepAliveManager().isStarted())
			stream.getKeepAliveManager().stop();
	}

}
