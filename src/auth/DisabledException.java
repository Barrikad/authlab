package auth;

public class DisabledException extends Exception {
	private static final long serialVersionUID = -5778449407673587451L;
	
	private String msg;

	public DisabledException(String msg) {
		this.msg = msg;
	}
	
	public String getError() {
		return msg;
	}
}
