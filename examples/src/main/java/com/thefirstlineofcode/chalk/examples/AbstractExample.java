package com.thefirstlineofcode.chalk.examples;

import com.thefirstlineofcode.basalt.xeps.ibr.IqRegister;
import com.thefirstlineofcode.basalt.xeps.ibr.RegistrationField;
import com.thefirstlineofcode.basalt.xeps.ibr.RegistrationForm;
import com.thefirstlineofcode.basalt.xmpp.core.IError;
import com.thefirstlineofcode.basalt.xmpp.core.JabberId;
import com.thefirstlineofcode.chalk.core.IChatClient;
import com.thefirstlineofcode.chalk.core.IErrorListener;
import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.core.stream.StandardStreamConfig;
import com.thefirstlineofcode.chalk.core.stream.StreamConfig;
import com.thefirstlineofcode.chalk.network.ConnectionException;
import com.thefirstlineofcode.chalk.network.ConnectionListenerAdapter;
import com.thefirstlineofcode.chalk.xeps.ibr.IRegistration;
import com.thefirstlineofcode.chalk.xeps.ibr.IRegistrationCallback;
import com.thefirstlineofcode.chalk.xeps.ibr.IbrPlugin;
import com.thefirstlineofcode.chalk.xeps.ibr.RegistrationException;

public abstract class AbstractExample extends ConnectionListenerAdapter implements Example, IErrorListener {
	protected Options options;
	protected boolean stop;
	
	public AbstractExample() {
		super();
		
		stop = false;
	}

	@Override
	public void init(Options options) {
		this.options = options;
		
		try {
			createUsers();
		} catch (RegistrationException e) {
			throw new RuntimeException("Can't create user.", e);
		}
		
		doInit();
	}

	protected void doInit() {}
	
	protected abstract String[][] getUserNameAndPasswords();

	protected StandardStreamConfig createStreamConfig(String resource) {
		StandardStreamConfig streamConfig = new StandardStreamConfig(options.host, options.port);
		streamConfig.setTlsPreferred(true);
		streamConfig.setResource(resource);
		
		streamConfig.setProperty(StreamConfig.PROPERTY_NAME_CHALK_MESSAGE_FORMAT, options.messageFormat);
		
		return streamConfig;
	}

	protected JabberId getJabberId(String user) {
		return getJabberId(user, null);
	}

	protected JabberId getJabberId(String user, String resource) {
		if (resource == null) {
			return JabberId.parse(String.format("%s@%s", user, options.host));			
		}
		
		return JabberId.parse(String.format("%s@%s/%s", user, options.host, resource));
	}

	protected StandardStreamConfig createStreamConfig() {
		return createStreamConfig("chalk_" + getExampleName() + "_example");
	}

	protected String getExampleName() {
		String className = getClass().getSimpleName();
		if (className.endsWith("Example")) {
			return className.substring(0, className.length() - 7);
		}
		
		throw new IllegalArgumentException("Can't determine example name. You should override getExampleName() method to resolve the problem.");
	}

	protected void createUsers() throws RegistrationException {
		String[][] userNameAndPaswords = getUserNameAndPasswords();
		if (userNameAndPaswords == null || userNameAndPaswords.length == 0)
			return;
		
		IChatClient chatClient = new StandardChatClient(createStreamConfig());
		chatClient.register(IbrPlugin.class);
		
		IRegistration registration = chatClient.createApi(IRegistration.class);
		registration.addConnectionListener(this);
		
		for (final String[] userNameAndPassword : userNameAndPaswords) {
			registration.register(new IRegistrationCallback() {
	
				@Override
				public Object fillOut(IqRegister iqRegister) {
					if (iqRegister.getRegister() instanceof RegistrationForm) {
						RegistrationForm form = new RegistrationForm();
						form.getFields().add(new RegistrationField("username", userNameAndPassword[0]));
						form.getFields().add(new RegistrationField("password", userNameAndPassword[1]));
						
						return form;
					} else {
						throw new RuntimeException("Can't get registration form.");
					}
				}
				
			});
		}
		
		chatClient.close();
	}

	@Override
	public void exceptionOccurred(ConnectionException exception) {
		printException(exception);
		
		if (exception.getType() == ConnectionException.Type.CONNECTION_CLOSED) {
			stop = true;
		}
	}

	@Override
	public void messageReceived(String message) {
		printString("<- " + message);
	}

	@Override
	public void messageSent(String message) {
		printString("-> " + message);
	}

	protected void printString(String string) {
		System.out.println(string);
	}

	protected void printException(Exception e) {
		System.out.println("Exception:");
		e.printStackTrace(System.out);
		System.out.println();
	}
	
	@Override
	public void occurred(IError error) {
		printError(error);
	}

	protected void printError(IError error) {
		System.out.println("Error:");
		System.out.println(String.format("'%s', '%s'.", error.getDefinedCondition(), error.getText()));
		System.out.println();
	}

}