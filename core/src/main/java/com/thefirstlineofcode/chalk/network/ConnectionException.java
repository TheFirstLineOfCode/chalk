package com.thefirstlineofcode.chalk.network;


public class ConnectionException extends Exception {
	private static final long serialVersionUID = -1333389331006451006L;
	
	public enum Type {
		ADDRESS_IS_UNRESOLVED,
		IO_ERROR,
		END_OF_STREAM,
		BAD_PROTOCOL_MESSAGE,
		OUT_OF_BUFFER,
		TLS_NOT_SUPPORTED,
		TLS_FAILURE,
		READ_RESPONSE_TIMEOUT,
		CONNECTION_CLOSED
	}
	
	private ConnectionException.Type type;

	public ConnectionException(ConnectionException.Type type) {
		super();
		this.type = type;
	}

	public ConnectionException(String message, ConnectionException.Type type) {
		super(message);
		this.type = type;
	}
	
	public ConnectionException(String message, ConnectionException.Type type, Throwable e) {
		super(message, e);
		this.type = type;
	}
	
	public ConnectionException(ConnectionException.Type type, Throwable e) {
		super(e);
		this.type = type;
	}
	
	public ConnectionException.Type getType() {
		return type;
	}
	
	@Override
	public String getMessage() {
		String message = super.getMessage();
		if (message != null) {
			return message;
		}
		
		return type.toString();
	}
}
