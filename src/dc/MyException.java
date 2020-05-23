package dc;

public class MyException extends Exception {

	 /**
	 * 
	 */
	private static final long serialVersionUID = 3972780898242732776L;
	public MyException() { super(); }
	  public MyException(String message) { super(message); }
	  public MyException(String message, Throwable cause) { super(message, cause); }
	  public MyException(Throwable cause) { super(cause); }
}

