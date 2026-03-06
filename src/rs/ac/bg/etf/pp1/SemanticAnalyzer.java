package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.exc.CompilerException;
import rs.ac.bg.etf.pp1.exc.ErrorCode;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {
	private static Logger log = Logger.getLogger(SemanticAnalyzer.class);	
	
	private Struct boolType = Tab.find("bool") != Tab.noObj ? Tab.find("bool").getType() : new Struct(Struct.Bool);

	Obj currentProgram = null;
	Boolean currentHasMainMethod = false;
	
	Struct currentType = null;
	Struct currentConstType = null;
	int currentConstValue;
		
		Obj currentEnum = null;
		int currentEnumValue;
		java.util.Set<Integer> currentEnumAssignedValues = new java.util.HashSet<>();
		
		Obj currentMethod = null;
		Struct currentMethodRetType = null;
		String currentMethodName = null;
		int currentMethodParamCount = 0;
		boolean currentMethodHasReturn = false;
		List<Struct> currentMethodFormalParamTypes = new ArrayList<>();
	int loopDepth = 0;
	int switchDepth = 0;
	List<java.util.Set<Integer>> switchCaseValues = new ArrayList<>();
	
	Boolean currentIsArray = false;

	int nVars;
	List<CompilerException> errors = new ArrayList<>();
	Map<SyntaxNode, Struct> nodeTypes = new IdentityHashMap<>();
	Map<Designator, Obj> designators = new IdentityHashMap<>();


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
	
	private boolean isPrimitiveType(Struct type) {
		return Tab.intType.equals(type) || Tab.charType.equals(type) || boolType.equals(type);
	}
	
	private boolean isIntLikeType(Struct type) {
		return Tab.intType.equals(type) || (type != null && type.getKind() == Struct.Enum);
	}
	
	private Struct getNodeType(SyntaxNode node) {
		if (node == null) {
			return Tab.noType;
		}
		Struct type = nodeTypes.get(node);
		return type != null ? type : Tab.noType;
	}
	
	private void setNodeType(SyntaxNode node, Struct type) {
		if (node != null) {
			nodeTypes.put(node, type);
		}
	}
	
	private void collectActArgTypes(ExprList exprList, List<Struct> out) {
		if (exprList instanceof ExprList2) {
			ExprList2 list2 = (ExprList2) exprList;
			collectActArgTypes(list2.getExprList(), out);
			out.add(getNodeType(list2.getExpr()));
			return;
		}
		
		if (exprList instanceof ExprList1) {
			out.add(getNodeType(((ExprList1) exprList).getExpr()));
		}
	}
	
	private List<Struct> getActArgTypes(OptCall1 call) {
		List<Struct> types = new ArrayList<>();
		if (call.getOptActPars() instanceof OptActPars1) {
			ActPars actPars = ((OptActPars1) call.getOptActPars()).getActPars();
			collectActArgTypes(actPars.getExprList(), types);
		}
		return types;
	}

	private List<Struct> getActArgTypes(OptActPars optActPars) {
		List<Struct> types = new ArrayList<>();
		if (optActPars instanceof OptActPars1) {
			ActPars actPars = ((OptActPars1) optActPars).getActPars();
			collectActArgTypes(actPars.getExprList(), types);
		}
		return types;
	}

	private List<Obj> getFormalParams(Obj methodObj) {
		List<Obj> params = new ArrayList<>();
		if (methodObj == null || methodObj == Tab.noObj) {
			return params;
		}
		int needed = methodObj.getLevel();
		for (Obj sym : methodObj.getLocalSymbols()) {
			if (params.size() >= needed) {
				break;
			}
			if (sym.getKind() == Obj.Var) {
				params.add(sym);
			}
		}
		return params;
	}

	private void validateCallArguments(Obj methodObj, OptActPars optActPars, SyntaxNode info) {
		List<Struct> actual = getActArgTypes(optActPars);
		if (methodObj == currentMethod) {
			if (actual.size() != currentMethodFormalParamTypes.size()) {
				log.error("Greska na liniji " + info.getLine() + ": neispravan broj argumenata za funkciju " + methodObj.getName());
				report_error(ErrorCode.TODO, info, methodObj.getName());
				return;
			}
			for (int i = 0; i < actual.size(); i++) {
				if (!actual.get(i).assignableTo(currentMethodFormalParamTypes.get(i))) {
					log.error("Greska na liniji " + info.getLine() + ": neispravan tip argumenta #" + (i + 1) + " za funkciju " + methodObj.getName());
					report_error(ErrorCode.TODO, info, methodObj.getName());
					return;
				}
			}
			return;
		}
		
		List<Obj> formal = getFormalParams(methodObj);
		if (actual.size() != formal.size()) {
			log.error("Greska na liniji " + info.getLine() + ": neispravan broj argumenata za funkciju " + methodObj.getName());
			report_error(ErrorCode.TODO, info, methodObj.getName());
			return;
		}
		for (int i = 0; i < actual.size(); i++) {
			if (!actual.get(i).assignableTo(formal.get(i).getType())) {
				log.error("Greska na liniji " + info.getLine() + ": neispravan tip argumenta #" + (i + 1) + " za funkciju " + methodObj.getName());
				report_error(ErrorCode.TODO, info, methodObj.getName());
				return;
			}
		}
	}
	
	private boolean validateBuiltInCall(Obj functionObj, OptActPars optActPars, SyntaxNode callSite) {
		String fn = functionObj.getName();
		if (!"ord".equals(fn) && !"chr".equals(fn) && !"len".equals(fn)) {
			return true;
		}
		
		List<Struct> args = getActArgTypes(optActPars);
		if (args.size() != 1) {
			log.error("Greska na liniji " + callSite.getLine() + ": funkcija " + fn + " mora imati tacno 1 argument");
			report_error(ErrorCode.TODO, callSite, fn);
			return false;
		}
		
		Struct argType = args.get(0);
		if ("ord".equals(fn) && !Tab.charType.equals(argType)) {
			log.error("Greska na liniji " + callSite.getLine() + ": ord zahteva argument tipa char");
			report_error(ErrorCode.TODO, callSite, fn);
			return false;
		}
		
		if ("chr".equals(fn) && !isIntLikeType(argType)) {
			log.error("Greska na liniji " + callSite.getLine() + ": chr zahteva argument tipa int");
			report_error(ErrorCode.TODO, callSite, fn);
			return false;
		}
		
		if ("len".equals(fn) && (argType == null || argType.getKind() != Struct.Array)) {
			log.error("Greska na liniji " + callSite.getLine() + ": len zahteva argument tipa niz");
			report_error(ErrorCode.TODO, callSite, fn);
			return false;
		}
		
		return true;
	}
	
	private boolean validateBuiltInCall(Obj functionObj, FactorDesignator callSite) {
		return validateBuiltInCall(functionObj, ((OptCall1) callSite.getOptCall()).getOptActPars(), callSite);
	}
	
	private Obj getDesignator(Designator designator) {
		Obj obj = designators.get(designator);
		return obj != null ? obj : Tab.noObj;
	}
	
	private boolean canAssignTo(Obj obj) {
		return obj.getKind() == Obj.Var || obj.getKind() == Obj.Elem || obj.getKind() == Obj.Fld;
	}
	
	private void report_symbol_usage(Designator designator, String symbolName, Obj symbolObj) {
		log.info("Pretraga na " + designator.getLine() + "(" + symbolName + "), nadjeno " + symbolObj);
	}
	
	private void report_undeclared_designator(Designator designator, String symbolName) {
		log.error("Greska na liniji " + designator.getLine() + ": ime " + symbolName + " nije deklarisano");
		report_error(ErrorCode.UNDECLARED_DESIGNATOR, designator, symbolName);
	}
	
	private void collectSuffixes(DesignatorSuffixList suffixList, List<DesignatorSuffix> out) {
		if (suffixList instanceof DesignatorSuffixList1) {
			DesignatorSuffixList1 list1 = (DesignatorSuffixList1) suffixList;
			collectSuffixes(list1.getDesignatorSuffixList(), out);
			out.add(list1.getDesignatorSuffix());
		}
	}

	private Obj resolveDesignator(Designator designator) {
		Obj currentObj = Tab.find(designator.getName());
		if (currentObj == Tab.noObj) {
			report_undeclared_designator(designator, designator.getName());
			return Tab.noObj;
		}
		
		report_symbol_usage(designator, designator.getName(), currentObj);
		
		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(designator.getDesignatorSuffixList(), suffixes);
		
		for (DesignatorSuffix suffix : suffixes) {
			if (currentObj == Tab.noObj) {
				return Tab.noObj;
			}
			
			if (suffix instanceof DesignatorSuffix3) {
				DesignatorSuffix3 arraySuffix = (DesignatorSuffix3) suffix;
				Struct indexType = getNodeType(arraySuffix.getExpr());
				if (!isIntLikeType(indexType)) {
					log.error("Greska na liniji " + designator.getLine() + ": indeks niza mora biti int");
					report_error(ErrorCode.TODO, designator, designator.getName());
					currentObj = Tab.noObj;
					continue;
				}
				
				if (currentObj.getType().getKind() != Struct.Array) {
					log.error("Greska na liniji " + designator.getLine() + ": " + currentObj.getName() + " nije niz");
					report_error(ErrorCode.TODO, designator, currentObj.getName());
					currentObj = Tab.noObj;
					continue;
				}
				
				currentObj = new Obj(Obj.Elem, "$elem", currentObj.getType().getElemType());
				continue;
			}
			
			if (suffix instanceof DesignatorSuffix2) {
				if (currentObj.getType().getKind() != Struct.Array) {
					log.error("Greska na liniji " + designator.getLine() + ": length se moze koristiti samo nad nizom");
					report_error(ErrorCode.TODO, designator, currentObj.getName());
					currentObj = Tab.noObj;
					continue;
				}
				
				currentObj = new Obj(Obj.Fld, "length", Tab.intType);
				continue;
			}
			
			if (suffix instanceof DesignatorSuffix1) {
				DesignatorSuffix1 dotSuffix = (DesignatorSuffix1) suffix;
				String memberName = dotSuffix.getField();
				
				if (currentObj.getKind() == Obj.Type && currentObj.getType().getKind() == Struct.Enum) {
					Obj enumMember = Tab.noObj;
					for (Obj member : currentObj.getLocalSymbols()) {
						if (member.getName().equals(memberName)) {
							enumMember = member;
							break;
						}
					}
					
					if (enumMember == Tab.noObj) {
						report_undeclared_designator(designator, memberName);
						currentObj = Tab.noObj;
						continue;
					}
					
					report_symbol_usage(designator, memberName, enumMember);
					currentObj = enumMember;
					continue;
				}
				
				log.error("Greska na liniji " + designator.getLine() + ": pristup clanovima podrzan je samo za enum u Nivo A");
				report_error(ErrorCode.TODO, designator, memberName);
				currentObj = Tab.noObj;
			}
		}
		
		return currentObj;
	}

	
	// Overridovanje visit metoda
	

	// PROGRAM
	@Override
	public void visit(Program program){
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(currentProgram);
		Tab.closeScope();
		
		if(!currentHasMainMethod) {
			log.error("Ne postoji main funkcija");
			report_error(ErrorCode.MAIN_METHOD_NOT_FOUND, program, "main");
		}
	}
	
	@Override
	public void visit(ProgramName programName){
		if(Tab.currentScope.findSymbol(programName.getName()) != null) {
			log.error("Dvostruka definicija programa " + programName.getName());
			report_error(ErrorCode.DUPLICATED_NAME, programName, programName.getName());
			currentProgram = Tab.noObj;
			return;
		}
		
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
		Obj constObj = Tab.currentScope.findSymbol(constInit.getName());
		if(constObj != null){
			log.error("Dvostruka definicija konstante " + constInit.getName());  
    		report_error(ErrorCode.DUPLICATED_NAME, constInit, constInit.getName());
			return;
		}
		if(!currentConstType.equals(currentType)){
			log.error("Neadekvatna dodela vrednosti konstante " + constInit.getName());
			report_error(ErrorCode.INVALID_CONSTANT_VALUE, constInit, constInit.getName());	  
			return;
		}
		constObj = Tab.insert(Obj.Con, constInit.getName(), currentType);
		constObj.setAdr(currentConstValue);
	}
	
	// VAR
	@Override
	public void visit(VarItem varItem){
		if(Tab.currentScope.findSymbol(varItem.getName()) != null) {
			log.error("Dvostruka definicija promenljive " + varItem.getName());
			report_error(ErrorCode.DUPLICATED_NAME, varItem, varItem.getName());
			return;
		}
		
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
		Obj enumObj = Tab.currentScope.findSymbol(enumName.getName());
		if(enumObj != null){
			log.error("Dvostruka definicija enuma " + enumName.getName());
			report_error(ErrorCode.DUPLICATED_NAME, enumName, enumName.getName());		
			return;
		}
			enumObj = Tab.insert(Obj.Type, enumName.getName(), new Struct(Struct.Enum));
			currentEnum = enumObj;
			currentEnumValue = 0;
			currentEnumAssignedValues.clear();
			Tab.openScope();
		}
	
	@Override
	public void visit(EnumItemNoAssign enumItem){
		Obj enumItemObj = Tab.currentScope.findSymbol(enumItem.getName());
		if(enumItemObj != null){
			log.error("Dvostruka definicija enum vrednosti " + enumItem.getName());
			report_error(ErrorCode.DUPLICATED_NAME, enumItem, enumItem.getName());	
			return;
		}
		while (currentEnumAssignedValues.contains(currentEnumValue)) {
			currentEnumValue++;
		}
		enumItemObj = Tab.insert(Obj.Con,  enumItem.getName(), currentEnum.getType());
		enumItemObj.setAdr(currentEnumValue);
		currentEnumAssignedValues.add(currentEnumValue);
		currentEnumValue++;
	}
	
	@Override
	public void visit(EnumItemWithAssign enumItem){
		Obj enumItemObj = Tab.currentScope.findSymbol(enumItem.getName());
		if(enumItemObj != null){
			log.error("Dvostruka definicija enum vrednosti " + enumItem.getName());
			report_error(ErrorCode.DUPLICATED_NAME, enumItem, enumItem.getName());	
			return;
		}
		if(currentEnumAssignedValues.contains(enumItem.getValue())){
			log.error("Duplirana vrednost enum item-a " + enumItem.getName());
			report_error(ErrorCode.INVALID_CONSTANT_VALUE, enumItem, enumItem.getName());
			return;
		}
		enumItemObj = Tab.insert(Obj.Con,  enumItem.getName(), currentEnum.getType());
		enumItemObj.setAdr(enumItem.getValue());
		currentEnumAssignedValues.add(enumItem.getValue());
		currentEnumValue = enumItem.getValue() + 1;
	}
	
	// METHOD
	@Override
	public void visit(MethodDecl methodDecl){
		if ("main".equals(currentMethodName) && currentMethodParamCount != 0) {
			log.error("Main funkcija mora biti bez argumenata");
			report_error(ErrorCode.INVALID_RETURN_TYPE, methodDecl, "main");
		}
		
		if (currentMethod != null) {
			if (currentMethod.getType() != Tab.noType && !currentMethodHasReturn) {
				log.error("Greska: metoda " + currentMethod.getName() + " nema return iskaz");
				report_error(ErrorCode.INVALID_RETURN_TYPE, methodDecl, currentMethod.getName());
			}
			currentMethod.setLevel(currentMethodParamCount);
		}
		
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		currentMethod = null;
		currentMethodName = null;
		currentMethodParamCount = 0;
		currentMethodHasReturn = false;
		currentMethodFormalParamTypes.clear();
	}
	
	@Override
	public void visit(MethodName methodName){
		currentMethodName = methodName.getName();
		currentMethodParamCount = 0;
		currentMethodHasReturn = false;
		currentMethodFormalParamTypes.clear();
		
		if(Tab.currentScope.findSymbol(currentMethodName) != null) {
			log.error("Dvostruka definicija funkcije " + currentMethodName);
			report_error(ErrorCode.DUPLICATED_NAME, methodName, currentMethodName);
			currentMethod = Tab.noObj;
			return;
		}
		
		if(currentMethodName.equals("main")){ 
			if(currentHasMainMethod) {
				log.error("Dvostruka definicija main funkcije");
				report_error(ErrorCode.DUPLICATED_NAME, methodName, methodName.getName());
			}
			else {
				currentHasMainMethod = true;
				if(currentMethodRetType != Tab.noType) {
					log.error("Pogresan povratni tip main funkcije");
					report_error(ErrorCode.INVALID_RETURN_TYPE, methodName, methodName.getName());
				}
			}
		}
		currentMethod = Tab.insert(Obj.Meth, currentMethodName, currentMethodRetType);
		currentMethod.setLevel(0);
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
		if(Tab.currentScope.findSymbol(formPar.getName()) != null) {
			log.error("Dvostruka definicija formalnog parametra " + formPar.getName());
			report_error(ErrorCode.DUPLICATED_NAME, formPar, formPar.getName());
			return;
		}
		
		if(currentIsArray)
			Tab.insert(Obj.Var, formPar.getName(), new Struct(Struct.Array, currentType));
		else
			Tab.insert(Obj.Var, formPar.getName(), currentType);
		
		currentMethodParamCount++;
		currentMethodFormalParamTypes.add(currentIsArray ? new Struct(Struct.Array, currentType) : currentType);
		if (currentMethod != null && currentMethod != Tab.noObj) {
			currentMethod.setLevel(currentMethodParamCount);
		}
	}

	
	// TYPE
	@Override
	public void visit(Type type){
		Obj typeObj = Tab.find(type.getType());
		if(typeObj == Tab.noObj){
			log.error("Nepostojeci tip " + type.getType() + " u tabeli simbola");
			report_error(ErrorCode.CANNOT_BE_RESOLVED_AS_TYPE, type, type.getType());
			currentType = Tab.noType;
			return;
		}
		if(typeObj.getKind() != Obj.Type){
			log.error("Neadekvatan tip " + type.getType() + " u tabeli simbola");
			report_error(ErrorCode.CANNOT_BE_RESOLVED_AS_TYPE, type, type.getType());
			currentType = Tab.noType;
			return;
		}
		currentType = typeObj.getType();
	}
	
	// DESIGNATOR
	@Override
	public void visit(Designator designator) {
		Obj resolved = resolveDesignator(designator);
		designators.put(designator, resolved);
	}
	
	// EXPRESSIONS
	@Override
	public void visit(FactorNumConst factorNumConst) {
		setNodeType(factorNumConst, Tab.intType);
	}
	
	@Override
	public void visit(FactorCharConst factorCharConst) {
		setNodeType(factorCharConst, Tab.charType);
	}
	
	@Override
	public void visit(FactorBoolConst factorBoolConst) {
		setNodeType(factorBoolConst, boolType);
	}
	
	@Override
	public void visit(FactorExpr factorExpr) {
		setNodeType(factorExpr, getNodeType(factorExpr.getExpr()));
	}
	
	@Override
	public void visit(FactorNewArr factorNewArr) {
		Struct exprType = getNodeType(factorNewArr.getExpr());
		if (!isIntLikeType(exprType)) {
			log.error("Greska na liniji " + factorNewArr.getLine() + ": velicina niza mora biti int");
			report_error(ErrorCode.TODO, factorNewArr, factorNewArr.getType().getType());
			setNodeType(factorNewArr, Tab.noType);
			return;
		}
		
		setNodeType(factorNewArr, new Struct(Struct.Array, currentType));
	}
	
	@Override
	public void visit(FactorDesignator factorDesignator) {
		Obj designatorObj = getDesignator(factorDesignator.getDesignator());
		if (designatorObj == Tab.noObj) {
			setNodeType(factorDesignator, Tab.noType);
			return;
		}
		
		if (factorDesignator.getOptCall() instanceof OptCall1) {
			if (designatorObj.getKind() != Obj.Meth) {
				log.error("Greska na liniji " + factorDesignator.getLine() + ": " + designatorObj.getName() + " nije funkcija");
				report_error(ErrorCode.TODO, factorDesignator, designatorObj.getName());
				setNodeType(factorDesignator, Tab.noType);
				return;
			}
			
			if (!validateBuiltInCall(designatorObj, factorDesignator)) {
				setNodeType(factorDesignator, Tab.noType);
				return;
			}
			if (!"ord".equals(designatorObj.getName()) && !"chr".equals(designatorObj.getName()) && !"len".equals(designatorObj.getName())) {
				validateCallArguments(designatorObj, ((OptCall1) factorDesignator.getOptCall()).getOptActPars(), factorDesignator);
			}
		}
		
		setNodeType(factorDesignator, designatorObj.getType());
	}
	
	@Override
	public void visit(Term1 term1) {
		setNodeType(term1, getNodeType(term1.getFactor()));
	}
	
	@Override
	public void visit(Term2 term2) {
		Struct leftType = getNodeType(term2.getTerm());
		Struct rightType = getNodeType(term2.getFactor());
		
		if (!isIntLikeType(leftType) || !isIntLikeType(rightType)) {
			log.error("Greska na liniji " + term2.getLine() + ": operand * / % mora biti int");
			report_error(ErrorCode.TODO, term2, "");
			setNodeType(term2, Tab.noType);
			return;
		}
		
		setNodeType(term2, Tab.intType);
	}
	
	@Override
	public void visit(SimpleExpr1 simpleExpr1) {
		setNodeType(simpleExpr1, getNodeType(simpleExpr1.getTerm()));
	}
	
	@Override
	public void visit(SimpleExpr2 simpleExpr2) {
		Struct termType = getNodeType(simpleExpr2.getTerm());
		if (!isIntLikeType(termType)) {
			log.error("Greska na liniji " + simpleExpr2.getLine() + ": unarni minus zahteva int");
			report_error(ErrorCode.TODO, simpleExpr2, "");
			setNodeType(simpleExpr2, Tab.noType);
			return;
		}
		
		setNodeType(simpleExpr2, Tab.intType);
	}
	
	@Override
	public void visit(SimpleExpr3 simpleExpr3) {
		Struct leftType = getNodeType(simpleExpr3.getSimpleExpr());
		Struct rightType = getNodeType(simpleExpr3.getTerm());
		
		if (!isIntLikeType(leftType) || !isIntLikeType(rightType)) {
			log.error("Greska na liniji " + simpleExpr3.getLine() + ": operand + ili - mora biti int");
			report_error(ErrorCode.TODO, simpleExpr3, "");
			setNodeType(simpleExpr3, Tab.noType);
			return;
		}
		
		setNodeType(simpleExpr3, Tab.intType);
	}
	
	@Override
	public void visit(Expr1 expr1) {
		setNodeType(expr1, getNodeType(expr1.getSimpleExpr()));
	}
	
	@Override
	public void visit(CondFact condFact) {
		Struct leftType = getNodeType(condFact.getSimpleExpr());
		
		if (condFact.getOptRel() instanceof OptRel1) {
			OptRel1 optRel = (OptRel1) condFact.getOptRel();
			Struct rightType = getNodeType(optRel.getSimpleExpr());
			if (!leftType.compatibleWith(rightType)) {
				log.error("Greska na liniji " + condFact.getLine() + ": nekompatibilni tipovi u relaciji");
				report_error(ErrorCode.TODO, condFact, "");
				setNodeType(condFact, Tab.noType);
				return;
			}
			
			if ((leftType.isRefType() || rightType.isRefType()) &&
					!(optRel.getRelop() instanceof RelopEQ || optRel.getRelop() instanceof RelopNEQ)) {
				log.error("Greska na liniji " + condFact.getLine() + ": za referentne tipove dozvoljeni su samo == i !=");
				report_error(ErrorCode.TODO, condFact, "");
				setNodeType(condFact, Tab.noType);
				return;
			}
			
			setNodeType(condFact, boolType);
			return;
		}
		
		setNodeType(condFact, leftType);
	}
	
	@Override
	public void visit(CondTerm1 condTerm1) {
		setNodeType(condTerm1, getNodeType(condTerm1.getCondFact()));
	}
	
	@Override
	public void visit(CondTerm2 condTerm2) {
		Struct leftType = getNodeType(condTerm2.getCondTerm());
		Struct rightType = getNodeType(condTerm2.getCondFact());
		
		if (!leftType.equals(boolType) || !rightType.equals(boolType)) {
			log.error("Greska na liniji " + condTerm2.getLine() + ": operand && mora biti bool");
			report_error(ErrorCode.TODO, condTerm2, "");
			setNodeType(condTerm2, Tab.noType);
			return;
		}
		
		setNodeType(condTerm2, boolType);
	}
	
	@Override
	public void visit(Condition1 condition1) {
		setNodeType(condition1, getNodeType(condition1.getCondTerm()));
	}
	
	@Override
	public void visit(Condition2 condition2) {
		Struct leftType = getNodeType(condition2.getCondition());
		Struct rightType = getNodeType(condition2.getCondTerm());
		
		if (!leftType.equals(boolType) || !rightType.equals(boolType)) {
			log.error("Greska na liniji " + condition2.getLine() + ": operand || mora biti bool");
			report_error(ErrorCode.TODO, condition2, "");
			setNodeType(condition2, Tab.noType);
			return;
		}
		
		setNodeType(condition2, boolType);
	}
	
	@Override
	public void visit(Expr2 expr2) {
		Struct condType = getNodeType(expr2.getCondFact());
		Struct trueType = getNodeType(expr2.getExpr());
		Struct falseType = getNodeType(expr2.getExpr1());
		
		if (!condType.equals(boolType)) {
			log.error("Greska na liniji " + expr2.getLine() + ": uslov ternarnog operatora mora biti bool");
			report_error(ErrorCode.TODO, expr2, "");
			setNodeType(expr2, Tab.noType);
			return;
		}
		
		if (!trueType.equals(falseType)) {
			log.error("Greska na liniji " + expr2.getLine() + ": grane ternarnog operatora moraju biti istog tipa");
			report_error(ErrorCode.TODO, expr2, "");
			setNodeType(expr2, Tab.noType);
			return;
		}
		
		setNodeType(expr2, trueType);
	}
	
	// STATEMENTS
	@Override
	public void visit(DesignatorStatement designatorStatement) {
		Obj designatorObj = getDesignator(designatorStatement.getDesignator());
		DesignatorStmtTail tail = designatorStatement.getDesignatorStmtTail();
		
		if (designatorObj == Tab.noObj) {
			return;
		}
		
		if (tail instanceof DesignatorStmtTail1) {
			if (!canAssignTo(designatorObj)) {
				log.error("Greska na liniji " + designatorStatement.getLine() + ": leva strana dodele mora biti promenljiva");
				report_error(ErrorCode.TODO, designatorStatement, designatorObj.getName());
				return;
			}
			
			Struct exprType = getNodeType(((DesignatorStmtTail1) tail).getExpr());
			if (!exprType.assignableTo(designatorObj.getType())) {
				log.error("Greska na liniji " + designatorStatement.getLine() + ": nekompatibilni tipovi u dodeli");
				report_error(ErrorCode.TODO, designatorStatement, designatorObj.getName());
			}
			return;
		}
		
		if (tail instanceof DesignatorStmtTail3 || tail instanceof DesignatorStmtTail4) {
			if (!canAssignTo(designatorObj)) {
				log.error("Greska na liniji " + designatorStatement.getLine() + ": ++/-- zahteva promenljivu");
				report_error(ErrorCode.TODO, designatorStatement, designatorObj.getName());
				return;
			}
			
			if (!Tab.intType.equals(designatorObj.getType())) {
				log.error("Greska na liniji " + designatorStatement.getLine() + ": ++/-- zahteva int tip");
				report_error(ErrorCode.TODO, designatorStatement, designatorObj.getName());
			}
			return;
		}
		
		if (tail instanceof DesignatorStmtTail2 && designatorObj.getKind() != Obj.Meth) {
			log.error("Greska na liniji " + designatorStatement.getLine() + ": " + designatorObj.getName() + " nije funkcija");
			report_error(ErrorCode.TODO, designatorStatement, designatorObj.getName());
		}
		
		if (tail instanceof DesignatorStmtTail2 && designatorObj.getKind() == Obj.Meth) {
			OptActPars optActPars = ((DesignatorStmtTail2) tail).getOptActPars();
			if (!validateBuiltInCall(designatorObj, optActPars, designatorStatement)) {
				return;
			}
			if (!"ord".equals(designatorObj.getName()) && !"chr".equals(designatorObj.getName()) && !"len".equals(designatorObj.getName())) {
				validateCallArguments(designatorObj, optActPars, designatorStatement);
			}
		}
	}
	
	@Override
	public void visit(StatementRead statementRead) {
		Obj designatorObj = getDesignator(statementRead.getDesignator());
		if (designatorObj == Tab.noObj) {
			return;
		}
		
		if (!canAssignTo(designatorObj)) {
			log.error("Greska na liniji " + statementRead.getLine() + ": read zahteva promenljivu");
			report_error(ErrorCode.TODO, statementRead, designatorObj.getName());
			return;
		}
		
		if (!isPrimitiveType(designatorObj.getType())) {
			log.error("Greska na liniji " + statementRead.getLine() + ": read podrzava samo int/char/bool");
			report_error(ErrorCode.TODO, statementRead, designatorObj.getName());
		}
	}
	
	@Override
	public void visit(StatementPrint statementPrint) {
		Struct exprType = getNodeType(statementPrint.getExpr());
		if (!isPrimitiveType(exprType) && !isIntLikeType(exprType)) {
			log.error("Greska na liniji " + statementPrint.getLine() + ": print podrzava samo int/char/bool");
			report_error(ErrorCode.TODO, statementPrint, "");
		}
	}
	
	@Override
	public void visit(StatementReturn statementReturn) {
		if (currentMethod == null || currentMethod == Tab.noObj) {
			log.error("Greska na liniji " + statementReturn.getLine() + ": return mora biti unutar funkcije");
			report_error(ErrorCode.INVALID_RETURN_TYPE, statementReturn, "");
			return;
		}
		currentMethodHasReturn = true;
		
		if (statementReturn.getOptExpr() instanceof OptExpr0) {
			if (currentMethod.getType() != Tab.noType) {
				log.error("Greska na liniji " + statementReturn.getLine() + ": return bez izraza u ne-void funkciji");
				report_error(ErrorCode.INVALID_RETURN_TYPE, statementReturn, currentMethod.getName());
			}
			return;
		}
		
		Struct exprType = getNodeType(((OptExpr1) statementReturn.getOptExpr()).getExpr());
		if (!exprType.assignableTo(currentMethod.getType())) {
			log.error("Greska na liniji " + statementReturn.getLine() + ": pogresan tip povratne vrednosti");
			report_error(ErrorCode.INVALID_RETURN_TYPE, statementReturn, currentMethod.getName());
		}
	}

	@Override
	public void visit(StatementBreak statementBreak) {
		if (loopDepth == 0 && switchDepth == 0) {
			log.error("Greska na liniji " + statementBreak.getLine() + ": break mora biti unutar for/switch");
			report_error(ErrorCode.TODO, statementBreak, "");
		}
	}
	
	@Override
	public void visit(StatementContinue statementContinue) {
		if (loopDepth == 0) {
			log.error("Greska na liniji " + statementContinue.getLine() + ": continue mora biti unutar for petlje");
			report_error(ErrorCode.TODO, statementContinue, "");
		}
	}

	@Override
	public void visit(StatementFor statementFor) {
		if (statementFor.getOptCondition() instanceof OptCondition1) {
			Struct condType = getNodeType(((OptCondition1) statementFor.getOptCondition()).getCondition());
			if (!condType.equals(boolType)) {
				log.error("Greska na liniji " + statementFor.getLine() + ": uslov for mora biti bool");
				report_error(ErrorCode.TODO, statementFor, "");
			}
		}
		loopDepth = Math.max(0, loopDepth - 1);
	}

	@Override
	public void visit(OptCondition0 optCondition0) {
		if (optCondition0.getParent() instanceof StatementFor) {
			loopDepth++;
		}
	}
	
	@Override
	public void visit(OptCondition1 optCondition1) {
		if (optCondition1.getParent() instanceof StatementFor) {
			loopDepth++;
		}
	}

	@Override
	public void visit(StatementSwitch statementSwitch) {
		Struct exprType = getNodeType(statementSwitch.getExpr());
		if (!isIntLikeType(exprType)) {
			log.error("Greska na liniji " + statementSwitch.getLine() + ": switch izraz mora biti int");
			report_error(ErrorCode.TODO, statementSwitch, "");
		}
		switchDepth = Math.max(0, switchDepth - 1);
		if (!switchCaseValues.isEmpty()) {
			switchCaseValues.remove(switchCaseValues.size() - 1);
		}
	}

	@Override
	public void visit(CaseList0 caseList0) {
		SyntaxNode p = caseList0.getParent();
		while (p != null && !(p instanceof StatementSwitch)) {
			p = p.getParent();
		}
		if (p instanceof StatementSwitch) {
			switchDepth++;
			switchCaseValues.add(new java.util.HashSet<>());
		}
	}

	@Override
	public void visit(Case caseNode) {
		if (switchCaseValues.isEmpty()) {
			return;
		}
		java.util.Set<Integer> used = switchCaseValues.get(switchCaseValues.size() - 1);
		if (used.contains(caseNode.getN1())) {
			log.error("Greska na liniji " + caseNode.getLine() + ": duplirana switch case vrednost " + caseNode.getN1());
			report_error(ErrorCode.TODO, caseNode, "");
		} else {
			used.add(caseNode.getN1());
		}
	}
	
	@Override
	public void visit(StatementIf statementIf) {
		Struct conditionType = getNodeType(statementIf.getCondition());
		if (!conditionType.equals(boolType)) {
			log.error("Greska na liniji " + statementIf.getLine() + ": uslov if mora biti bool");
			report_error(ErrorCode.TODO, statementIf, "");
		}
	}
	
}
