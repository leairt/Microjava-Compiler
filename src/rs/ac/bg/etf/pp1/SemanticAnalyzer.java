package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.exc.CompilerException;
import rs.ac.bg.etf.pp1.exc.ErrorCode;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {
	private static Logger log = Logger.getLogger(SemanticAnalyzer.class);	
	
	Struct boolType = Tab.find("bool").getType();

	Obj currentProgram = null;
	Boolean currentHasMainMethod = false;
	
	Struct currentType = null;
	Struct currentConstType = null;
	int currentConstValue;
		
	Obj currentEnum = null;
	int currentEnumValue;
	
	Obj currentMethod = null;
	Struct currentMethodRetType = null;
	String currentMethodName = null;
	
	Boolean currentIsArray = false;

	int nVars;
	List<CompilerException> errors = new ArrayList<>();


	public int getnVars() {
		return nVars;
	}

    // errors

    public void report_error(ErrorCode errorCode, SyntaxNode info, String data) {
		int line = (info == null) ? 0: info.getLine();		
        errors.add(new CompilerException(errorCode, line, data));        
	}

    public java.util.List<CompilerException> getErrors() {
    	return errors;
    }

	
	public boolean passed(){
    	return errors.isEmpty();
    }

	
	// Overridovanje visit metoda
	
	
	// PROGRAM
	@Override
	public void visit(Program program){
		Tab.chainLocalSymbols(currentProgram);
		Tab.closeScope();
	}
	
	@Override
	public void visit(ProgramName programName){
		currentProgram = Tab.insert(Obj.Prog, programName.getName(), Tab.noType);
		Tab.openScope();
	}
	
	// CONST VALUES
	@Override
	public void visit(ConstValueBool constValueBool){
		currentConstValue = constValueBool.getValue();
		currentConstType = boolType;
	}
	
	@Override
	public void visit(ConstValueChar constValueChar){
		currentConstValue = constValueChar.getValue();
		currentConstType = Tab.charType;
	}
	
	@Override
	public void visit(ConstValueNum constValueNum){
		currentConstValue = constValueNum.getValue();
		currentConstType = Tab.intType;
	}
	
	// CONST
	@Override
	public void visit(ConstInit constInit){
		Obj constObj = Tab.find(constInit.getName());
		if(constObj != Tab.noObj){
			log.error("Dvostruka definicija enum vrednosti " + constInit.getName());  
    		report_error(ErrorCode.DUPLICATED_NAME, constInit, constInit.getName());
			return;
		}
		if(!currentConstType.assignableTo(currentType)){
			log.error("Neadekvatna dodela vrednosti konstante " + constInit.getName());
			report_error(ErrorCode.TODO, constInit, constInit.getName());	  
			return;
		}
		constObj = Tab.insert(Obj.Con, constInit.getName(), currentType);
		constObj.setAdr(currentConstValue);
	}
	
	// VAR
	@Override
	public void visit(VarItem varItem){
		if(currentIsArray)
			Tab.insert(Obj.Var, varItem.getName(), new Struct(Struct.Array, currentType));
		else
			Tab.insert(Obj.Var, varItem.getName(), currentType);
	}
	
	@Override
	public void visit(OptArray0 optArray0){
		currentIsArray = false;
	}
	
	@Override
	public void visit(OptArray1 optArray1){
		currentIsArray = true;
	}
	
	// ENUM
	@Override
	public void visit(EnumDecl enumDecl){
		Tab.chainLocalSymbols(currentEnum);
		Tab.closeScope();
	}
	
	@Override
	public void visit(EnumName enumName){
		Obj enumObj = Tab.find(enumName.getName());
		if(enumObj != Tab.noObj){
			log.error("Dvostruka definicija enuma " + enumName.getName());
			report_error(ErrorCode.DUPLICATED_NAME, enumName, enumName.getName());		
			return;
		}
		enumObj = Tab.insert(Obj.Type, enumName.getName(), new Struct(Struct.Enum));
		currentEnum = enumObj;
		currentEnumValue = 0;
		Tab.openScope();
	}
	
	@Override
	public void visit(EnumItemNoAssign enumItem){
		Obj enumItemObj = Tab.find(enumItem.getName());
		if(enumItemObj != Tab.noObj){
			log.error("Dvostruka definicija enum vrednosti " + enumItem.getName());
			report_error(ErrorCode.DUPLICATED_NAME, enumItem, enumItem.getName());	
			return;
		}
		enumItemObj = Tab.insert(Obj.Con,  enumItem.getName(), currentEnum.getType());
		enumItemObj.setAdr(currentEnumValue);
		currentEnumValue++;
	}
	
	@Override
	public void visit(EnumItemWithAssign enumItem){
		Obj enumItemObj = Tab.find(enumItem.getName());
		if(enumItemObj != Tab.noObj){
			log.error("Dvostruka definicija enum vrednosti " + enumItem.getName());
			report_error(ErrorCode.DUPLICATED_NAME, enumItem, enumItem.getName());	
			return;
		}
		if(enumItem.getValue() < currentEnumValue){
			log.error("Neadekvatna vrednost enum item-a " + enumItem.getName());
			report_error(ErrorCode.TODO, enumItem, enumItem.getName());
			return;
		}
		enumItemObj = Tab.insert(Obj.Con,  enumItem.getName(), currentEnum.getType());
		enumItemObj.setAdr(enumItem.getValue());
		currentEnumValue = enumItem.getValue() + 1;
	}
	
	// METHOD
	@Override
	public void visit(MethodDecl methodDecl){
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
	}
	
	@Override
	public void visit(MethodName methodName){
		currentMethodName = methodName.getName();
		if(currentMethodName.equals("main")){ 
			if(currentHasMainMethod) {
				// report_error("Dvostruka definicija main funkcije", methodName);
				log.error("Dvostruka definicija main funkcije");
			}
			else {
				currentHasMainMethod = true;
				if(currentMethodRetType != Tab.noType) {
					// report_error("Pogresan povratni tip main funkcije", methodName);
					log.error("Pogresan povratni tip main funkcije");
					report_error(ErrorCode.INVALID_RETURN_TYPE, methodName, methodName.getName());
				}
			}
		}
		currentMethod = Tab.insert(Obj.Meth, currentMethodName, currentMethodRetType);
		methodName.obj = currentMethod;
		Tab.openScope();
	}
	
	@Override
	public void visit(MethodRetTypeVoid methodRetType){
		currentMethodRetType = Tab.noType;
	}
	
	@Override
	public void visit(MethodRetTypeObject methodRetType){
		currentMethodRetType = currentType;
	}
	
	// FORMAL PARAMETER
	@Override
	public void visit(FormPar formPar){
		if(currentIsArray)
			Tab.insert(Obj.Var, formPar.getI2(), new Struct(Struct.Array, currentType));
		else
			Tab.insert(Obj.Var, formPar.getI2(), currentType);
	}

	
	// TYPE
	@Override
	public void visit(Type type){
		Obj typeObj = Tab.find(type.getType());
		if(typeObj == Tab.noObj){
			// report_error("Nepostojeci tip " + type.getType() + " u tabeli simbola! ", type);
			log.error("Nepostojeci tip " + type.getType() + " u tabeli simbola");
			report_error(ErrorCode.CANNOT_BE_RESOLVED_AS_TYPE, type, type.getType());
			currentType = Tab.noType;
			return;
		}
		if(typeObj.getKind() != Obj.Type){
			// report_error("Neadekvatan tip " + type.getType() + " u tabeli simbola! ", type);
			log.error("Neadekvatan tip " + type.getType() + " u tabeli simbola");
			report_error(ErrorCode.CANNOT_BE_RESOLVED_AS_TYPE, type, type.getType());
			currentType = Tab.noType;
			return;
		}
		currentType = typeObj.getType();
	}
	
	// 
	
	/*public void visit(VarDecl varDecl){
		//varDeclCount++;
		//report_info("Deklarisana promenljiva "+ varDecl.getVarName(), varDecl);
		//Obj varNode = Tab.insert(Obj.Var, varDecl.getVarName(), varDecl.getType().struct);
	}
	
    public void visit(StatementPrint print) {
		//printCallCount++;
	}
    
    public void visit(ProgName progName){
    	progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
    	Tab.openScope();
    }
    
    public void visit(Program program){
    	nVars = Tab.currentScope.getnVars();
    	Tab.chainLocalSymbols(program.getProgName().obj);
    	Tab.closeScope();
    }
    
    public void visit(Type type){
    	Obj typeNode = Tab.find(type.getTypeName());
    	if(typeNode == Tab.noObj){
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! ", null);
    		type.struct = Tab.noType;
    	}else{
    		if(Obj.Type == typeNode.getKind()){
    			type.struct = typeNode.getType();
    		}else{
    			report_error("Greska: Ime " + type.getTypeName() + " ne predstavlja tip!", type);
    			type.struct = Tab.noType;
    		}
    	}
    }
    
    public void visit(MethodTypeName methodTypeName){
    	currentMethod = Tab.insert(Obj.Meth, methodTypeName.getMethName(), methodTypeName.getType().struct);
    	methodTypeName.obj = currentMethod;
    	Tab.openScope();
		report_info("Obradjuje se funkcija " + methodTypeName.getMethName(), methodTypeName);
    }
    
    public void visit(MethodDecl methodDecl){
    	if(!returnFound && currentMethod.getType() != Tab.noType){
			report_error("Semanticka greska na liniji " + methodDecl.getLine() + ": funkcija " + currentMethod.getName() + " nema return iskaz!", null);
    	}
    	Tab.chainLocalSymbols(currentMethod);
    	Tab.closeScope();
    	
    	returnFound = false;
    	currentMethod = null;
    }
    
    public void visit(Designator designator){
    	Obj obj = Tab.find(designator.getName());
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getName()+" nije deklarisano! ", null);
    	}
    	designator.obj = obj;
    }
    
    
    public void visit(FuncCall funcCall){
    	Obj func = funcCall.getDesignator().obj;
    	if(Obj.Meth == func.getKind()){
			report_info("Pronadjen poziv funkcije " + func.getName() + " na liniji " + funcCall.getLine(), null);
			funcCall.struct = func.getType();
    	}else{
			report_error("Greska na liniji " + funcCall.getLine()+" : ime " + func.getName() + " nije funkcija!", null);
			funcCall.struct = Tab.noType;
    	}
    }
    
    public void visit(Term term){
    	term.struct = term.getFactor().struct;
    }
    
    public void visit(TermExpr termExpr){
    	termExpr.struct = termExpr.getTerm().struct;
    }
    
    public void visit(AddExpr addExpr){
    	Struct te = addExpr.getExpr().struct;
    	Struct t = addExpr.getTerm().struct;
    	if(te.equals(t) && te == Tab.intType){
    		addExpr.struct = te;
    	}else{
			report_error("Greska na liniji "+ addExpr.getLine()+" : nekompatibilni tipovi u izrazu za sabiranje.", null);
			addExpr.struct = Tab.noType;
    	}
    }
    
    public void visit(Const cnst){
    	cnst.struct = Tab.intType;
    }
    
    public void visit(Var var){
    	var.struct = var.getDesignator().obj.getType();
    }
    
    public void visit(ReturnExpr returnExpr){
    	returnFound = true;
    	Struct currMethType = currentMethod.getType();
    	if(!currMethType.compatibleWith(returnExpr.getExpr().struct)){
			report_error("Greska na liniji " + returnExpr.getLine() + " : " + "tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije " + currentMethod.getName(), null);
    	}
    }
    
    public void visit(Assignment assignment){
    	if(!assignment.getExpr().struct.assignableTo(assignment.getDesignator().obj.getType()))
    		report_error("Greska na liniji " + assignment.getLine() + " : " + "nekompatibilni tipovi u dodeli vrednosti! ", null);
    }*/
    
    
}
