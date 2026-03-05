package rs.ac.bg.etf.pp1.exc;

public enum ErrorCode {
	// lexic error
	LEXIC_ERROR(100),
	
	// syntax error
	SYNTAX_ERROR(200),
	UNRECOVERED_SYNTAX_ERROR(201),

	// semantic errors
	CANNOT_BE_RESOLVED_AS_TYPE(300),
	UNDECLARED_DESIGNATOR(301),
	DUPLICATED_NAME(302),
	INVALID_CONSTANT_VALUE(303),
	MAIN_METHOD_NOT_FOUND(304),
	INVALID_RETURN_TYPE(305),
	
	
	// technical error
	TECHNICAL_ERROR(900),

	// unknown error
	TODO(999);

	int code;
	
	ErrorCode(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
}