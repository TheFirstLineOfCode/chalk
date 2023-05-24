package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.xmpp.core.stream.sasl.Failure;

public class AuthFailureException extends Exception {
	private static final long serialVersionUID = 8052873857188360437L;
	
	private Failure.ErrorCondition errorCondition;

	public AuthFailureException() {
		this(null);
	}
	
	public AuthFailureException(Failure.ErrorCondition errorCondition) {
		super();
		
		this.errorCondition = errorCondition;
	}
	
	public Failure.ErrorCondition getErrorCondition() {
		return errorCondition;
	}
}
