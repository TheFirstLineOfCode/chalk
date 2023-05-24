package com.thefirstlineofcode.chalk.examples.lite;

import com.thefirstlineofcode.basalt.xeps.ibr.IqRegister;
import com.thefirstlineofcode.basalt.xeps.ibr.RegistrationField;
import com.thefirstlineofcode.basalt.xeps.ibr.RegistrationForm;
import com.thefirstlineofcode.chalk.core.IChatClient;
import com.thefirstlineofcode.chalk.core.StandardChatClient;
import com.thefirstlineofcode.chalk.examples.AbstractLiteExample;
import com.thefirstlineofcode.chalk.xeps.ibr.IRegistration;
import com.thefirstlineofcode.chalk.xeps.ibr.IRegistrationCallback;
import com.thefirstlineofcode.chalk.xeps.ibr.IbrPlugin;

public class IbrLiteExample extends AbstractLiteExample {
	private IChatClient chatClient;
	
	@Override
	public void run() throws Exception {
		chatClient = new StandardChatClient(createStreamConfig(null));
		chatClient.register(IbrPlugin.class);
		
		try {			
			IRegistration registration = chatClient.createApi(IRegistration.class);
			registration.addConnectionListener(this);
			registration.register(new IRegistrationCallback() {
				
				@Override
				public Object fillOut(IqRegister iqRegister) {
					if (iqRegister.getRegister() instanceof RegistrationForm) {
						RegistrationForm form = new RegistrationForm();
						form.getFields().add(new RegistrationField("username", "dongger"));
						form.getFields().add(new RegistrationField("password", "a_stupid_man"));
						
						return form;
					} else {
						throw new RuntimeException("Can't get registration form.");
					}
				}
				
			});
		} finally {			
			chatClient.close();
		}
	}
	
	@Override
	protected String[][] getUserNameAndPasswords() {
		return null;
	}
}
