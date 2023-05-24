package com.thefirstlineofcode.chalk.core.stream;

import com.thefirstlineofcode.basalt.xmpp.core.IError;

public class NegotiationException extends RuntimeException {

	private static final long serialVersionUID = 5834897402216642798L;
	
	private IStreamNegotiant source;
	private Object additionalErrorInfo;
	
	public NegotiationException(IStreamNegotiant source) {
		this(null, (IStreamNegotiant)source);
	}
	
	public NegotiationException(IStreamNegotiant source, Object additionalErrorInfo) {
		this(null, source, additionalErrorInfo);
	}

	public NegotiationException(String message, IStreamNegotiant source, Object additionalErrorInfo) {
		super(message);
		
		this.source = source;
		this.additionalErrorInfo = additionalErrorInfo;
	}
	
	public Object getAdditionalErrorInfo() {
		return additionalErrorInfo;
	}
	
	public IStreamNegotiant getSource() {
		return source;
	}
	
	@Override
	public String getMessage() {
		String message = super.getMessage();
		if (message != null)
			return message;
		
		return String.format("Negotiation exception. Source: %s. %s.", source, getAdditionalErrorInfoString());
	}

	private String getAdditionalErrorInfoString() {
		if (additionalErrorInfo == null)
			return "Additional error info: null";
		
		if (!(additionalErrorInfo instanceof IError))
			return "Additional error info: " + additionalErrorInfo.toString();
		
		StringBuilder sb = new StringBuilder();
		sb.append("Additional error info: ");
		
		IError error = (IError)additionalErrorInfo;
		if (error.getDefinedCondition() != null) {
			sb.append("defined-condition = ").append(error.getDefinedCondition());
		} else {				
			if (error.getApplicationSpecificCondition() != null)
				sb.append("appplication-specific-condition = ").append(error.getApplicationSpecificCondition().toString());
		}
		
		if (error.getText() == null)
			return sb.toString();
		
		if (sb.length() != 0)
			sb.append(", ");
		
		sb.append("text = ").append(error.getText().getText());
		
		if (sb.charAt(sb.length() - 1) == '.')
			sb.deleteCharAt(sb.length() - 1);
		
		return sb.toString();
	}
}
