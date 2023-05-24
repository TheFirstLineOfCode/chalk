package com.thefirstlineofcode.chalk.xeps.ibr;

public class RegistrationException extends Exception {
	
	private static final long serialVersionUID = 3398560808264161877L;
	
	private IbrError error;
	private Throwable cause;
	
	public RegistrationException(IbrError error) {
		this(error, null);
	}
	
	public RegistrationException(IbrError error, Throwable cause) {
		this.error = error;
		this.cause = cause;
	}

	public IbrError getError() {
		return error;
	}

	public Throwable getCause() {
		return cause;
	}
	
	@Override
	public String getMessage() {
		return error.toString();
	}
	
}
