package rs.ac.bg.etf.pp1.exc;

public class CompilerException extends Exception {

	private Integer lineNumber;
	private ErrorCode errorCode;
	private String data;
	private Object refObj;
	
	public CompilerException(String message, Throwable cause) {
		super(message, cause);
		this.errorCode = ErrorCode.TECHNICAL_ERROR;
	}

	public CompilerException(String message) {
		super(message);
		this.errorCode = ErrorCode.TECHNICAL_ERROR;
	}

	public CompilerException(Throwable cause) {
		super(cause);
		this.errorCode = ErrorCode.TECHNICAL_ERROR;
	}

	public CompilerException(ErrorCode errorCode) {
		super(errorCode.name());
		this.errorCode = errorCode;
	}

	public CompilerException(ErrorCode errorCode, int lineNumber) {
		super(errorCode.name());
		this.errorCode = errorCode;
		this.lineNumber = lineNumber;
	}

	public CompilerException(ErrorCode errorCode, int lineNumber, String data) {
		super(errorCode.name());
		this.errorCode = errorCode;
		this.lineNumber = lineNumber;
		this.data = data;
	}

	public Integer getLineNumber() {
		return lineNumber;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public String getData() {
		return data;
	}

	public Object getRefObj() {
		return refObj;
	}

	@Override
	public String toString() {
		return "CompilerException: " + getMessage() + " [lineNumber=" + lineNumber + ", errorCode=" + errorCode + ", data=" + data
				+ ", refObj=" + refObj + "]";
	}
}