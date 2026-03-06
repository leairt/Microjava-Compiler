package rs.ac.bg.etf.pp1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import java_cup.runtime.Symbol;
import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.exc.CompilerException;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.mj.runtime.Run;
import rs.etf.pp1.mj.runtime.disasm;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class Compiler {
	Logger log = Logger.getLogger(getClass());

	private Reader reader;
	
	private Yylex lexer;
	private MJParser parser;
	private List<CompilerException> errors;
	private String outputString = null;
	private String inputString = "";
	 
	private static Map<String, Object> tabState = null;
	private static Map<String, Object> codeState = null;
	
	public Compiler(InputStream in) {
		this.reader = new InputStreamReader(in);
		this.errors = new ArrayList<>();
	
		// save or restore Tab class state
		if(tabState == null) 
	    	tabState = StateHelper.saveState(Tab.class);
		else
			StateHelper.restoreState(Tab.class, tabState);
		
		// save or restore Code class state
		if(codeState == null)
			codeState = StateHelper.saveState(Code.class);
		else
			StateHelper.restoreState(Code.class, codeState);
	}
	
	public Compiler(Reader reader) {
		this.reader = reader; 
		this.errors = new ArrayList<>();
	}

	public void build() {
		try {
			errors.clear();
						
			lexer = new Yylex(reader);
			
			parser = new MJParser(lexer);
			
	        Symbol s = parser.parse();  // pocetak parsiranja
	        errors.addAll(lexer.getErrors());
			errors.addAll(parser.getErrors());
			
			if(!errors.isEmpty())
				return;
						
	        Program prog = (Program)(s.value); 
	        	    	
			// ispis sintaksnog stabla
			log.info(prog.toString(""));

			// inicializacija tabele simbola
	        Tab.init();
	        
	        // mora da se registruje bool tip (fali u tabeli simbola)
	    	if(Tab.find("bool") == Tab.noObj) {
				Obj bullObj = Tab.insert(Obj.Type, "bool", new Struct(Struct.Bool));
				bullObj.setLevel(-1);
				bullObj.setAdr(-1);
	    	}

			// ispis prepoznatih programskih konstrukcija
	    	SemanticAnalyzer semanticAnalizer = new SemanticAnalyzer();
			prog.traverseBottomUp(semanticAnalizer); 	      		
			Tab.dump();
			
			errors.addAll(semanticAnalizer.getErrors());
			if(!errors.isEmpty())
				return;
			
			// generisanje koda
			CodeGenerator codeGenerator = new CodeGenerator();
			prog.traverseBottomUp(codeGenerator);
					
			String objFileName = "obj/" + codeGenerator.getProgramName() + ".obj";
			File objFile = new File(objFileName);
			if(objFile.exists())
				objFile.delete();

			Code.dataSize = semanticAnalizer.getnVars();
			Code.mainPc = codeGenerator.getMainPC();
			Code.write(new FileOutputStream(objFile));
			
			log.info("Generisanje koda završeno (" + objFileName + ")");
			
			System.out.println("\n=====================DISASEMBLED CODE==========================");
			disasm.main(new String[] {objFileName, "-debug"});

			System.out.println("\n=====================RUNNING PROGRAM===========================");

			try(OutputRecorder outputRecorder = new OutputRecorder(inputString)) {
				// Run.main(new String[] {objFileName, "-debug"});
				Run.main(new String[] {objFileName});
				outputString = outputRecorder.toString();
			}
			// ispišemo i na System.out (zato što smo ga presreli, pa se nije ništa ispisivalo)
			System.out.println(outputString);
			System.out.println("\n=====================DONE=====================================\n");
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
			errors.add(new CompilerException(e.getMessage(), e));
		}
    }
	
	public List<CompilerException> getErrors() {
		return errors; 
	}

	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	public String[] getOutput() {
		return outputString.split("\n");
	}

	public void setInput(String input) {
		this.inputString = input != null ? input : "";
	}
	
	
	protected static class OutputRecorder implements Closeable {
		InputStream originalSysIn;
        PrintStream originalSysOut;	        
        PrintStream originalSysErr;	        
        ByteArrayOutputStream outputBuffer;
        PrintStream customSysOut;
        		
		public OutputRecorder(String input) {
			originalSysIn = System.in;
	        originalSysOut = System.out;	        
	        originalSysErr = System.err;
	        outputBuffer = new ByteArrayOutputStream();
	        customSysOut = new PrintStream(outputBuffer, true);

	        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
	        System.setOut(customSysOut);
			System.setErr(customSysOut);
		}
		@Override
		public void close() throws IOException {
			customSysOut.close();
			outputBuffer.close();
			
			// vratimo stare vrednosti
			System.setIn(originalSysIn);
			System.setOut(originalSysOut);
			System.setErr(originalSysErr);
		}
		@Override
		public String toString() {
			return outputBuffer.toString();
		}
	}
	
	// ova klasa je potrebna da sačuvamo stanje Tab i Code zato što imaju statičke atribute 
	// pa obrade koje se pozivaju sukcesivno imaju pogrešno stanje a same klase Tab i Code nemaju 
	// implementiranje "reset" operacije koja vraća podešavanja statičkih variabli na originalne vrednosti
	
	protected static class StateHelper {
	    public static Map<String, Object> saveState(Class<?> tabClass) {
	        Map<String, Object> state = new HashMap<>();

	        for (Field field : tabClass.getDeclaredFields()) {
	            int modifiers = field.getModifiers();

	            if (Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
	                try {
	                    field.setAccessible(true);
	                    state.put(field.getName(), field.get(null));
	                } catch (IllegalAccessException e) {
	                    throw new RuntimeException("Failed to save field: " + field.getName(), e);
	                }
	            }
	        }
	        return state;
	    }

	    public static void restoreState(Class<?> tabClass, Map<String, Object> state) {
	        for (Field field : tabClass.getDeclaredFields()) {
	            int modifiers = field.getModifiers();

	            if (Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
	                if (state.containsKey(field.getName())) {
	                    try {
	                        field.setAccessible(true);
	                        field.set(null, state.get(field.getName()));
	                    } catch (IllegalAccessException e) {
	                        throw new RuntimeException("Failed to restore field: " + field.getName(), e);
	                    }
	                }
	            }
	        }
	    } 
	} 
}
