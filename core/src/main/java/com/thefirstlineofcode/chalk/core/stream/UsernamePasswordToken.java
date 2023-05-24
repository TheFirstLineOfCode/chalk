package com.thefirstlineofcode.chalk.core.stream;

public class UsernamePasswordToken implements IAuthenticationToken {
	private String username;
	private char[] password;
	
	public UsernamePasswordToken(String username, String password) {
		this(username, password.toCharArray());
	}
	
	public UsernamePasswordToken(String username, char[] password) {
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		return username;
	}
	
	public char[] getPassword() {
		return password;
	}
	
	@Override
	public Object getPrincipal() {
		return getUsername();
	}

	@Override
	public Object getCredentials() {
		return getPassword();
	}

}
