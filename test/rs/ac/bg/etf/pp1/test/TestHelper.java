package rs.ac.bg.etf.pp1.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.Compiler;
import rs.ac.bg.etf.pp1.exc.CompilerException;
import rs.ac.bg.etf.pp1.exc.ErrorCode;
import rs.ac.bg.etf.pp1.util.Log4JUtils;

public class TestHelper {
	private static Logger log = Logger.getLogger(TestHelper.class);

	private Compiler compiler;
	
	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}

	public TestHelper() {
	}

	public void execute(String testID) {
		if(compiler != null && compiler.getErrors() != null)
			compiler.getErrors().clear();
		
		File sourceCode = new File("junit/" + testID + ".mj");

		log.info("Compiling source file: " + sourceCode.getAbsolutePath());
		try(FileInputStream fis = new FileInputStream(sourceCode)) {
			compiler = new Compiler(fis);
			compiler.build();
			
			if(compiler.hasErrors()) {				
				log.debug("dump errors");
				for(CompilerException error: compiler.getErrors()) {
					log.debug(error);
				}

				return;
			}
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
			compiler.getErrors().add(new CompilerException(e.getMessage(), e));
		}				
	}

	public boolean hasErrors() {
		return !compiler.getErrors().isEmpty();
	}
	
	public boolean hasError(ErrorCode errorCode) {
		for(CompilerException error: compiler.getErrors()) 
			if(errorCode == error.getErrorCode())
				return true;
		return false;
	}
	
	public boolean hasError(ErrorCode errorCode, int lineNumber) {
		for(CompilerException error: compiler.getErrors())
			if(errorCode == error.getErrorCode() 
				&& error.getLineNumber() != null && error.getLineNumber().intValue() == lineNumber)
				
				return true;
		return false;
	}
	
	public boolean hasError(ErrorCode errorCode, String data) {
		for(CompilerException error: compiler.getErrors())
			if(errorCode == error.getErrorCode() 
				&& error.getData() != null && error.getData().equals(data))
				
				return true;
		return false;
	}

	public boolean hasError(ErrorCode errorCode, int lineNumber, String data) {
		for(CompilerException error: compiler.getErrors())
			if(errorCode == error.getErrorCode() 
				&& error.getLineNumber() != null && error.getLineNumber().intValue() == lineNumber
				&& error.getData() != null && error.getData().equals(data))

				return true;
		return false;
	}

	public String[] getOutput() {		
		return compiler.getOutput();
	}
}