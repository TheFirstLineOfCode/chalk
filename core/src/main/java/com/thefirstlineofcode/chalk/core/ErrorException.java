package com.thefirstlineofcode.chalk.core;

import com.thefirstlineofcode.basalt.xmpp.core.IError;

public class ErrorException extends Exception {
	private static final long serialVersionUID = 6739863800967990840L;
	
	private IError error;

	public ErrorException(IError error) {
		this(error.getDefinedCondition(), error);
	}

	public ErrorException(String message, IError error) {
		super(message);
		this.error = error;
	}
	
	public IError getError() {
		return error;
	}
	
	@Override
	public String getMessage() {
		return String.format("ProtocolException['%s', '%s', '%s']", error.getDefinedCondition(),
			(error.getText() == null ? null : error.getText().getText()),
					error.getApplicationSpecificCondition());
	}
	
	@Override
	public String toString() {
		return getMessage();
	}
	
}
