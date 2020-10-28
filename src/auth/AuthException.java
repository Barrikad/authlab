package auth;

public class AuthException extends Exception {
	private String msg;

	private static final long serialVersionUID = -2087486966441589996L;

	public AuthException(String msg) {
		this.msg = msg;
	}
	
	public String getError() {
		return msg;
	}
}