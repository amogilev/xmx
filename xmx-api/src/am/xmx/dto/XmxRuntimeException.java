package am.xmx.dto;

/**
 * Exceptions thrown from XmxService.  
 */
public class XmxRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -4412106011072900393L;

	public XmxRuntimeException(String message) {
		super(message);
	}

	public XmxRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public XmxRuntimeException(Throwable cause) {
		super(cause);
	}

}
