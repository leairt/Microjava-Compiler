package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	Set<Integer> currentEnumAssignedValues = new HashSet<>();
		

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

	int nVars; // brojač globalnih promenljivih

	List<CompilerException> errors = new ArrayList<>();
	
	Map<SyntaxNode, Struct> nodeTypes = new IdentityHashMap<>(); // (čvor, izračunati tip) za izraze i uslove
	Map<Designator, Obj> designators = new IdentityHashMap<>(); // (designator, razrešen simbol) za sve designatore


	public int getnVars() {
		return nVars;
	}

    // ERRORS

    public void report_error(ErrorCode errorCode, SyntaxNode info, String data) {
		int line = (info == null) ? 0: info.getLine();		
        errors.add(new CompilerException(errorCode, line, data));        
	}

    public List<CompilerException> getErrors() {
    	return errors;
    }
	
	public boolean passed(){
    	return errors.isEmpty();
    }
	
	
	// POMOĆNE FUNKCIJE
	
	// primitivni tipovi su int, char i bool
	private boolean isPrimitiveType(Struct type) {
		return Tab.intType.equals(type) || Tab.charType.equals(type) || boolType.equals(type);
	}
	
	// int-like tipovi su int i enum
	private boolean isIntLikeType(Struct type) {
		return Tab.intType.equals(type) || (type != null && type.getKind() == Struct.Enum);
	}
	
	// getter i setter za nodeType
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
	
	// rekurzivna funkcija za skupljanje tipova stvarnih argumenata funkcije
	private void collectActArgTypes(ExprList exprList, List<Struct> out) {
		// ExprList je levo-rekurzivan, pa skupljamo argumente redom kojim se pojavljuju u programu (a ne obrnutim redom kao u AST-u)
		
		// više od 1 argumenta
		if (exprList instanceof ExprList2) {
			ExprList2 list2 = (ExprList2) exprList;
			collectActArgTypes(list2.getExprList(), out);
			out.add(getNodeType(list2.getExpr()));
			return;
		}
		
		// 1 argument
		if (exprList instanceof ExprList1) {
			out.add(getNodeType(((ExprList1) exprList).getExpr()));
		}
	}
	
	// izvlači tipove stvarnih argumenata funkcije iz AST čvora i vraća ih kao listu
	private List<Struct> getActArgTypes(OptActPars optActPars) {
		List<Struct> types = new ArrayList<>();
		
		if (optActPars instanceof OptActPars1) { // ima argumente
			ActPars actPars = ((OptActPars1) optActPars).getActPars();
			collectActArgTypes(actPars.getExprList(), types);
		}
		
		return types;
	}
	
	// izvlači formalne parametre funkcije iz njenog Obj-a i vraća ih kao listu
	private List<Obj> getFormalParams(Obj methodObj) {
		List<Obj> params = new ArrayList<>();
		
		if (methodObj == null || methodObj == Tab.noObj) {
			return params;
		}
		
		// formalni parametri su u localSymbols i imaju kind == Obj.Var, a njihov broj je jednak level metode
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
		
		// uslovi: broj stvarnih i formalnih mora biti isti
		// 		   svaki stvarni argument mora biti assignable na formalni tip
		 
		// specijalno: u slučaju rekurzije, metoda se poziva sama unutar svog tela, pa se njen Obj jos uvek ne nalazi u Tab-u
		//             i nema finalno chain-ovane formalne parametre
		//             zato, ako je metoda pozvana sama unutar svog tela, koristimo currentMethodFormalParamTypes koje punimo u visit(FormPar)

		List<Struct> actual = getActArgTypes(optActPars);
		
		if (methodObj == currentMethod) { 	// metoda je pozvana sama unutar svog tela
			if (actual.size() != currentMethodFormalParamTypes.size()) {
				log.error("Greska na liniji " + info.getLine() + ": neispravan broj argumenata za funkciju " + methodObj.getName());
				report_error(ErrorCode.SEMANTIC_ERROR, info, methodObj.getName());
				return;
			}
			for (int i = 0; i < actual.size(); i++) {
				if (!actual.get(i).assignableTo(currentMethodFormalParamTypes.get(i))) {
					log.error("Greska na liniji " + info.getLine() + ": neispravan tip argumenta " + (i + 1) + " za funkciju " + methodObj.getName());
					report_error(ErrorCode.SEMANTIC_ERROR, info, methodObj.getName());
					return;
				}
			}
			return;
		}
		
		List<Obj> formal = getFormalParams(methodObj);
		
		if (actual.size() != formal.size()) { 
			log.error("Greska na liniji " + info.getLine() + ": neispravan broj argumenata za funkciju " + methodObj.getName());
			report_error(ErrorCode.SEMANTIC_ERROR, info, methodObj.getName());
			return;
		}

		for (int i = 0; i < actual.size(); i++) {
			if (!actual.get(i).assignableTo(formal.get(i).getType())) {
				log.error("Greska na liniji " + info.getLine() + ": neispravan tip argumenta " + (i + 1) + " za funkciju " + methodObj.getName());
				report_error(ErrorCode.SEMANTIC_ERROR, info, methodObj.getName());
				return;
			}
		}
	}
	
	// validira poziv ugrađene funkcije (ord, chr, len) na osnovu njenog Obj-a i stvarnih argumenata
	private boolean validateBuiltInCall(Obj functionObj, OptActPars optActPars, SyntaxNode callSite) {
		String fn = functionObj.getName();
		
		if (!"ord".equals(fn) && !"chr".equals(fn) && !"len".equals(fn)) {
			return true;
		}
		
		List<Struct> args = getActArgTypes(optActPars);
		
		if (args.size() != 1) {
			log.error("Greska na liniji " + callSite.getLine() + ": funkcija " + fn + " mora imati tacno 1 argument");
			report_error(ErrorCode.SEMANTIC_ERROR, callSite, fn);
			return false;
		}
		
		Struct argType = args.get(0);
		
		// ord(char) -> int
		if ("ord".equals(fn) && !Tab.charType.equals(argType)) {
			log.error("Greska na liniji " + callSite.getLine() + ": ord zahteva argument tipa char");
			report_error(ErrorCode.SEMANTIC_ERROR, callSite, fn);
			return false;
		}
		
		// chr(int) -> char
		if ("chr".equals(fn) && !isIntLikeType(argType)) {
			log.error("Greska na liniji " + callSite.getLine() + ": chr zahteva argument tipa int");
			report_error(ErrorCode.SEMANTIC_ERROR, callSite, fn);
			return false;
		}
		
		// len(array) -> int
		if ("len".equals(fn) && (argType == null || argType.getKind() != Struct.Array)) {
			log.error("Greska na liniji " + callSite.getLine() + ": len zahteva argument tipa niz");
			report_error(ErrorCode.SEMANTIC_ERROR, callSite, fn);
			return false;
		}
		
		return true;
	}
	
	// validira poziv ugrađene funkcije (ord, chr, len) na osnovu njenog Obj-a i AST čvora
	private boolean validateBuiltInCall(Obj functionObj, FactorDesignator callSite) {
		return validateBuiltInCall(functionObj, ((OptCall1) callSite.getOptCall()).getOptActPars(), callSite);
	}
	
	// getter za razrešeni designator
	private Obj getDesignator(Designator designator) {
		Obj obj = designators.get(designator);
		return obj != null ? obj : Tab.noObj;
	}
	
	// provera da li se Obj može dodeliti (Var, Elem, Fld)
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
	
	// rekurzivna funkcija za skupljanje sufiksa designatora redom kojim se pojavljuju u programu (a ne obrnutim redom kao u AST-u)
	private void collectSuffixes(DesignatorSuffixList suffixList, List<DesignatorSuffix> out) {
		if (suffixList instanceof DesignatorSuffixList1) { // ima sufikse
			DesignatorSuffixList1 list1 = (DesignatorSuffixList1) suffixList;
			collectSuffixes(list1.getDesignatorSuffixList(), out);
			out.add(list1.getDesignatorSuffix());
		}
	}

	// funkcija koja razrešava designator na osnovu njegovog imena i sufiksa, proverava pravila i vraća razrešeni Obj
	private Obj resolveDesignator(Designator designator) {
		Obj currentObj = Tab.find(designator.getName()); // pronalazimo simbol na osnovu imena designatora
		
		if (currentObj == Tab.noObj) {
			report_undeclared_designator(designator, designator.getName());
			return Tab.noObj;
		}
		
		report_symbol_usage(designator, designator.getName(), currentObj);
		
		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(designator.getDesignatorSuffixList(), suffixes); // skupljamo sufikse designatora u listu redom kojim se pojavljuju u programu
		
		for (DesignatorSuffix suffix : suffixes) {
			if (currentObj == Tab.noObj) {
				return Tab.noObj;
			}
			
			if (suffix instanceof DesignatorSuffix3) { // član niza: designator[expr]
				DesignatorSuffix3 arraySuffix = (DesignatorSuffix3) suffix;
				Struct indexType = getNodeType(arraySuffix.getExpr());
				if (!isIntLikeType(indexType)) {
					log.error("Greska na liniji " + designator.getLine() + ": indeks niza mora biti int");
					report_error(ErrorCode.SEMANTIC_ERROR, designator, designator.getName());
					currentObj = Tab.noObj;
					continue;
				}
				
				if (currentObj.getType().getKind() != Struct.Array) {
					log.error("Greska na liniji " + designator.getLine() + ": " + currentObj.getName() + " nije niz");
					report_error(ErrorCode.SEMANTIC_ERROR, designator, currentObj.getName());
					currentObj = Tab.noObj;
					continue;
				}
				
				currentObj = new Obj(Obj.Elem, "$elem", currentObj.getType().getElemType());
				continue;
			}
			
			if (suffix instanceof DesignatorSuffix2) { // dohvatanje dužine: designator.length
				if (currentObj.getType().getKind() != Struct.Array) {
					log.error("Greska na liniji " + designator.getLine() + ": length se moze koristiti samo nad nizom");
					report_error(ErrorCode.SEMANTIC_ERROR, designator, currentObj.getName());
					currentObj = Tab.noObj;
					continue;
				}
				
				currentObj = new Obj(Obj.Fld, "length", Tab.intType);
				continue;
			}
			
			if (suffix instanceof DesignatorSuffix1) { // pristup polju: designator.field
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
				report_error(ErrorCode.SEMANTIC_ERROR, designator, memberName);
				currentObj = Tab.noObj;
			}
		}
		
		return currentObj;
	}

	
	// Override-ovanje visit metoda
	

	// PROGRAM
	@Override
	public void visit(Program program){
		// yatvaramo globalni opseg programa i vezujemo sve lokalne simbole za Obj.Prog.
		nVars = Tab.currentScope.getnVars();
		Tab.chainLocalSymbols(currentProgram);
		Tab.closeScope();
		
		// mora da postoji main metoda
		if(!currentHasMainMethod) {
			log.error("Greska na liniji " + program.getLine() + ": ne postoji main funkcija");
			report_error(ErrorCode.MAIN_METHOD_NOT_FOUND, program, "main");
		}
	}
	
	@Override
	public void visit(ProgramName programName){
		if(Tab.currentScope.findSymbol(programName.getName()) != null) {
			log.error("Greska na liniji " + programName.getLine() + ": dvostruka definicija programa " + programName.getName());
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
		// uslovi: ime ne sme vec postojati u istom opsegu
		//         value mora biti istog tipa kao deklarisani Type
		
		Obj constObj = Tab.currentScope.findSymbol(constInit.getName());
		
		if(constObj != null){
			log.error("Greska na liniji " + constInit.getLine() + ": dvostruka definicija konstante " + constInit.getName());  
    		report_error(ErrorCode.DUPLICATED_NAME, constInit, constInit.getName());
			return;
		}
		if(!currentConstType.equals(currentType)){
			log.error("Greska na liniji " + constInit.getLine() + ": neadekvatna dodela vrednosti konstante " + constInit.getName());
			report_error(ErrorCode.INVALID_CONSTANT_VALUE, constInit, constInit.getName());	  
			return;
		}
		constObj = Tab.insert(Obj.Con, constInit.getName(), currentType);
		constObj.setAdr(currentConstValue);
	}
	
	// VAR
	@Override
	public void visit(VarItem varItem){
		// uslov: ne sme da postoji dupla deklaracija u istom opsegu
		if(Tab.currentScope.findSymbol(varItem.getName()) != null) {
			log.error("Greska na liniji " + varItem.getLine() + ": dvostruka definicija promenljive " + varItem.getName());
			report_error(ErrorCode.DUPLICATED_NAME, varItem, varItem.getName());
			return;
		}
		
		if(currentIsArray)
			// niz je tipa new Struct(Struct.Array,elemType=currentType)
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
		// enum je nov tip i ima svoj interni opseg za item-e
		Obj enumObj = Tab.currentScope.findSymbol(enumName.getName());
		if(enumObj != null){
			log.error("Greska na liniji " + enumName.getLine() + ": dvostruka definicija enuma " + enumName.getName());
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
		// ako vrednost nije eksplicitna, uzima se sledeća
		Obj enumItemObj = Tab.currentScope.findSymbol(enumItem.getName());
		if(enumItemObj != null){
			log.error("Greska na liniji " + enumItem.getLine() + ": dvostruka definicija enum item-a " + enumItem.getName());
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
		// eksplicitna enum vrednost mora biti veća od prethodne, a enum item jedinstven u opsegu
		Obj enumItemObj = Tab.currentScope.findSymbol(enumItem.getName());
		if(enumItemObj != null){
			log.error("Greska na liniji " + enumItem.getLine() + ": dvostruka definicija enum item-a " + enumItem.getName());
			report_error(ErrorCode.DUPLICATED_NAME, enumItem, enumItem.getName());	
			return;
		}
		if(enumItem.getValue() < currentEnumValue){
			log.error("Greska na liniji " + enumItem.getLine() + ": vrednost enum item-a " + enumItem.getName() + " mora biti veca od prethodne");
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
		// na kraju metode: 
	    //		validiramo da li je main funkcija bez argumenata
		// 		level = broj formalnih parametara metode
		// 		vezujemo formalne parametre za Obj.Meth i zatvaramo opseg metode

		if ("main".equals(currentMethodName) && currentMethodParamCount != 0) {
			log.error("Greska na liniji " + methodDecl.getLine() + ": main funkcija ne sme imati argumente");
			report_error(ErrorCode.INVALID_RETURN_TYPE, methodDecl, "main");
		}
		
		if (currentMethod != null) {
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
		// na poceku metode: resetujemo stanje, kreiramo Obj.Meth i otvaramo opseg
		currentMethodName = methodName.getName();
		currentMethodParamCount = 0;
		currentMethodHasReturn = false;
		currentMethodFormalParamTypes.clear();
		
		if(Tab.currentScope.findSymbol(currentMethodName) != null) {
			log.error("Greska na liniji " + methodName.getLine() + ": dvostruka definicija funkcije " + currentMethodName);
			report_error(ErrorCode.DUPLICATED_NAME, methodName, currentMethodName);
			currentMethod = Tab.noObj;
			return;
		}
		
		// provere za main funkciju: mora biti void i ne sme imati parametre
		if(currentMethodName.equals("main")){ 
			if(currentHasMainMethod) {
				log.error("Greska na liniji " + methodName.getLine() + ": dvostruka definicija main funkcije");
				report_error(ErrorCode.DUPLICATED_NAME, methodName, methodName.getName());
			}
			else {
				currentHasMainMethod = true;
				if(currentMethodRetType != Tab.noType) {
					log.error("Greska na liniji " + methodName.getLine() + ": pogresan povratni tip main funkcije");
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
		// formalni parametar je lokalna promenljiva metode
		// dodatno ga pamtimo i u currentMethodFormalParamTypes zbog rekurzivnih poziva
		 
		if(Tab.currentScope.findSymbol(formPar.getName()) != null) {
			log.error("Greska na liniji " + formPar.getLine() + ": dvostruka definicija formalnog parametra " + formPar.getName());
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
			log.error("Greska na liniji " + type.getLine() + ": nepostojeci tip " + type.getType() + " u tabeli simbola");
			report_error(ErrorCode.CANNOT_BE_RESOLVED_AS_TYPE, type, type.getType());
			currentType = Tab.noType;
			return;
		}
		
		if(typeObj.getKind() != Obj.Type){
			log.error("Greska na liniji " + type.getLine() + ": neadekvatan tip " + type.getType() + " u tabeli simbola");
			report_error(ErrorCode.CANNOT_BE_RESOLVED_AS_TYPE, type, type.getType());
			currentType = Tab.noType;
			return;
		}
		
		currentType = typeObj.getType();
	}
	
	// DESIGNATOR
	@Override
	public void visit(Designator designator) {
		Obj resolved = resolveDesignator(designator); // razrešavamo designator
		designators.put(designator, resolved); // čuvamo ga u mapu designators
	}
	
	// FACTOR
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
			report_error(ErrorCode.SEMANTIC_ERROR, factorNewArr, factorNewArr.getType().getType());
			setNodeType(factorNewArr, Tab.noType);
			return;
		}
		
		setNodeType(factorNewArr, new Struct(Struct.Array, currentType));
	}
	
	@Override
	public void visit(FactorDesignator factorDesignator) {
		// designator kao factor može biti ili vrednost ili poziv funkcije
		
		Obj designatorObj = getDesignator(factorDesignator.getDesignator());
		
		if (designatorObj == Tab.noObj) {
			setNodeType(factorDesignator, Tab.noType);
			return;
		}
		
		if (factorDesignator.getOptCall() instanceof OptCall1) { // poziv funkcije
			if (designatorObj.getKind() != Obj.Meth) {
				log.error("Greska na liniji " + factorDesignator.getLine() + ": " + designatorObj.getName() + " nije funkcija");
				report_error(ErrorCode.SEMANTIC_ERROR, factorDesignator, designatorObj.getName());
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
	
	// TERM
	@Override
	public void visit(Term1 term1) { // jednostavan term: factor
		setNodeType(term1, getNodeType(term1.getFactor()));
	}
	
	@Override
	public void visit(Term2 term2) { // term sa operatorom: term *,/,% factor
		Struct leftType = getNodeType(term2.getTerm());
		Struct rightType = getNodeType(term2.getFactor());
		
		if (!isIntLikeType(leftType) || !isIntLikeType(rightType)) {
			log.error("Greska na liniji " + term2.getLine() + ": operandi za *, /, i % moraju biti tipa int");
			report_error(ErrorCode.SEMANTIC_ERROR, term2, "");
			setNodeType(term2, Tab.noType);
			return;
		}
		
		setNodeType(term2, Tab.intType);
	}
	
	// SIMPLE EXPRESSION
	@Override
	public void visit(SimpleExpr1 simpleExpr1) { // jednostavan izraz: term
		setNodeType(simpleExpr1, getNodeType(simpleExpr1.getTerm()));
	}
	
	@Override
	public void visit(SimpleExpr2 simpleExpr2) { // unarni minus: -term
		Struct termType = getNodeType(simpleExpr2.getTerm());
		if (!isIntLikeType(termType)) {
			log.error("Greska na liniji " + simpleExpr2.getLine() + ": unarni minus zahteva tip int");
			report_error(ErrorCode.SEMANTIC_ERROR, simpleExpr2, "");
			setNodeType(simpleExpr2, Tab.noType);
			return;
		}
		
		setNodeType(simpleExpr2, Tab.intType);
	}
	
	@Override
	public void visit(SimpleExpr3 simpleExpr3) { // izraz sa operatorom: simpleExpr +,- term
		Struct leftType = getNodeType(simpleExpr3.getSimpleExpr());
		Struct rightType = getNodeType(simpleExpr3.getTerm());
		
		if (!isIntLikeType(leftType) || !isIntLikeType(rightType)) {
			log.error("Greska na liniji " + simpleExpr3.getLine() + ": operandi + ili - moraju biti tipa int");
			report_error(ErrorCode.SEMANTIC_ERROR, simpleExpr3, "");
			setNodeType(simpleExpr3, Tab.noType);
			return;
		}
		
		setNodeType(simpleExpr3, Tab.intType);
	}
	
	@Override
	public void visit(Expr1 expr1) { // jednostavan izraz: simpleExpr
		setNodeType(expr1, getNodeType(expr1.getSimpleExpr()));
	}
	
	// CONDITION FACT
	@Override
	public void visit(CondFact condFact) {
		// CondFact je ili prost izraz ili relacija (expr relop expr)
		Struct leftType = getNodeType(condFact.getSimpleExpr());
		
		if (condFact.getOptRel() instanceof OptRel1) {
			OptRel1 optRel = (OptRel1) condFact.getOptRel();
			Struct rightType = getNodeType(optRel.getSimpleExpr());
			if (!leftType.compatibleWith(rightType)) {
				log.error("Greska na liniji " + condFact.getLine() + ": nekompatibilni tipovi u relaciji");
				report_error(ErrorCode.SEMANTIC_ERROR, condFact, "");
				setNodeType(condFact, Tab.noType);
				return;
			}
			
			if ((leftType.isRefType() || rightType.isRefType()) &&
					!(optRel.getRelop() instanceof RelopEQ || optRel.getRelop() instanceof RelopNEQ)) {
				log.error("Greska na liniji " + condFact.getLine() + ": za referentne tipove dozvoljeni su samo == i !=");
				report_error(ErrorCode.SEMANTIC_ERROR, condFact, "");
				setNodeType(condFact, Tab.noType);
				return;
			}
			
			setNodeType(condFact, boolType);
			return;
		}
		
		setNodeType(condFact, leftType);
	}
	
	// CONDITION TERM
	@Override
	public void visit(CondTerm1 condTerm1) { // jednostavan uslov: condFact
		setNodeType(condTerm1, getNodeType(condTerm1.getCondFact()));
	}
	
	@Override
	public void visit(CondTerm2 condTerm2) { // uslov sa relacijom: condTerm && condFact
		Struct leftType = getNodeType(condTerm2.getCondTerm());
		Struct rightType = getNodeType(condTerm2.getCondFact());
		
		if (!leftType.equals(boolType) || !rightType.equals(boolType)) {
			log.error("Greska na liniji " + condTerm2.getLine() + ": operand && mora biti tipa bool");
			report_error(ErrorCode.SEMANTIC_ERROR, condTerm2, "");
			setNodeType(condTerm2, Tab.noType);
			return;
		}
		
		setNodeType(condTerm2, boolType);
	}
	
	// CONDITION
	@Override
	public void visit(Condition1 condition1) { // jednostavan uslov: condTerm
		setNodeType(condition1, getNodeType(condition1.getCondTerm()));
	}
	
	@Override
	public void visit(Condition2 condition2) { // uslov sa relacijom: condition || condTerm
		Struct leftType = getNodeType(condition2.getCondition());
		Struct rightType = getNodeType(condition2.getCondTerm());
		
		if (!leftType.equals(boolType) || !rightType.equals(boolType)) {
			log.error("Greska na liniji " + condition2.getLine() + ": operand || mora biti tipa bool");
			report_error(ErrorCode.SEMANTIC_ERROR, condition2, "");
			setNodeType(condition2, Tab.noType);
			return;
		}
		
		setNodeType(condition2, boolType);
	}
	
	// EXPRESSION
	@Override
	public void visit(Expr2 expr2) { // ternarni operator(if u jednom redu): expr ? expr : expr
		// uslovi: uslov mora biti bool, a grane obe istog tipa
		Struct condType = getNodeType(expr2.getCondFact());
		Struct trueType = getNodeType(expr2.getExpr());
		Struct falseType = getNodeType(expr2.getExpr1());
		
		if (!condType.equals(boolType)) {
			log.error("Greska na liniji " + expr2.getLine() + ": uslov ternarnog operatora mora biti tipa bool");
			report_error(ErrorCode.SEMANTIC_ERROR, expr2, "");
			setNodeType(expr2, Tab.noType);
			return;
		}
		
		if (!trueType.equals(falseType)) {
			log.error("Greska na liniji " + expr2.getLine() + ": grane ternarnog operatora moraju biti istog tipa");
			report_error(ErrorCode.SEMANTIC_ERROR, expr2, "");
			setNodeType(expr2, Tab.noType);
			return;
		}
		
		setNodeType(expr2, trueType);
	}
	
	// STATEMENT
	@Override
	public void visit(DesignatorStatement designatorStatement) {
		// DesignatorStatement pokriva: dodelu, poziv funkcije, ++, --
		 
		Obj designatorObj = getDesignator(designatorStatement.getDesignator());
		DesignatorStmtTail tail = designatorStatement.getDesignatorStmtTail();
		
		if (designatorObj == Tab.noObj) {
			return;
		}
		
		if (tail instanceof DesignatorStmtTail1) { // dodela: designator = expr
			if (!canAssignTo(designatorObj)) {
				log.error("Greska na liniji " + designatorStatement.getLine() + ": leva strana dodele mora biti promenljiva");
				report_error(ErrorCode.SEMANTIC_ERROR, designatorStatement, designatorObj.getName());
				return;
			}
			
			Struct exprType = getNodeType(((DesignatorStmtTail1) tail).getExpr());
			if (!exprType.assignableTo(designatorObj.getType())) {
				log.error("Greska na liniji " + designatorStatement.getLine() + ": nekompatibilni tipovi u dodeli");
				report_error(ErrorCode.SEMANTIC_ERROR, designatorStatement, designatorObj.getName());
			}
			return;
		}
		
		if (tail instanceof DesignatorStmtTail3 || tail instanceof DesignatorStmtTail4) { // designator++ ili designator--
			if (!canAssignTo(designatorObj)) {
				log.error("Greska na liniji " + designatorStatement.getLine() + ": ++/-- zahteva promenljivu");
				report_error(ErrorCode.SEMANTIC_ERROR, designatorStatement, designatorObj.getName());
				return;
			}
			
			if (!Tab.intType.equals(designatorObj.getType())) {
				log.error("Greska na liniji " + designatorStatement.getLine() + ": ++/-- zahteva int tip");
				report_error(ErrorCode.SEMANTIC_ERROR, designatorStatement, designatorObj.getName());
			}
			return;
		}
		
		if (tail instanceof DesignatorStmtTail2 && designatorObj.getKind() != Obj.Meth) {
			log.error("Greska na liniji " + designatorStatement.getLine() + ": " + designatorObj.getName() + " nije funkcija");
			report_error(ErrorCode.SEMANTIC_ERROR, designatorStatement, designatorObj.getName());
		}
		
		if (tail instanceof DesignatorStmtTail2 && designatorObj.getKind() == Obj.Meth) { // poziv funkcije: designator(optActPars)
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
		// read prima samo primitivne l-vrednosti (int, char, bool)
		
		Obj designatorObj = getDesignator(statementRead.getDesignator());
		
		if (designatorObj == Tab.noObj) {
			return;
		}
		
		if (!canAssignTo(designatorObj)) {
			log.error("Greska na liniji " + statementRead.getLine() + ": read zahteva promenljivu");
			report_error(ErrorCode.SEMANTIC_ERROR, statementRead, designatorObj.getName());
			return;
		}
		
		if (!isPrimitiveType(designatorObj.getType())) {
			log.error("Greska na liniji " + statementRead.getLine() + ": read podrzava samo int, char i bool");
			report_error(ErrorCode.SEMANTIC_ERROR, statementRead, designatorObj.getName());
		}
	}
	
	@Override
	public void visit(StatementPrint statementPrint) {
		// print prima samo primitivne l-vrednosti (int, char, bool)

		Struct exprType = getNodeType(statementPrint.getExpr());
		
		if (!isPrimitiveType(exprType) && !isIntLikeType(exprType)) {
			log.error("Greska na liniji " + statementPrint.getLine() + ": print podrzava samo int, char i bool");
			report_error(ErrorCode.SEMANTIC_ERROR, statementPrint, "");
		}
	}
	
	@Override
	public void visit(StatementReturn statementReturn) {
		// return van metode je greska
		if (currentMethod == null || currentMethod == Tab.noObj) {
			log.error("Greska na liniji " + statementReturn.getLine() + ": return mora biti unutar funkcije");
			report_error(ErrorCode.INVALID_RETURN_TYPE, statementReturn, "");
			return;
		}
		currentMethodHasReturn = true;
		
		if (statementReturn.getOptExpr() instanceof OptExpr0) { // return bez izraza (za void funkcije)
			if (currentMethod.getType() != Tab.noType) {
				log.error("Greska na liniji " + statementReturn.getLine() + ": return bez izraza u ne-void funkciji");
				report_error(ErrorCode.INVALID_RETURN_TYPE, statementReturn, currentMethod.getName());
			}
			return;
		}
		
		Struct exprType = getNodeType(((OptExpr1) statementReturn.getOptExpr()).getExpr()); // return sa izrazom
		if (!exprType.assignableTo(currentMethod.getType())) {
			log.error("Greska na liniji " + statementReturn.getLine() + ": pogresan tip povratne vrednosti");
			report_error(ErrorCode.INVALID_RETURN_TYPE, statementReturn, currentMethod.getName());
		}
	}

	@Override
	public void visit(StatementBreak statementBreak) {
		// break van for ili switch je greska
		if (loopDepth == 0 && switchDepth == 0) {
			log.error("Greska na liniji " + statementBreak.getLine() + ": break mora biti unutar for/switch");
			report_error(ErrorCode.SEMANTIC_ERROR, statementBreak, "");
		}
	}
	
	@Override
	public void visit(StatementContinue statementContinue) {
		// continue van for petlje je greska
		if (loopDepth == 0) {
			log.error("Greska na liniji " + statementContinue.getLine() + ": continue mora biti unutar for petlje");
			report_error(ErrorCode.SEMANTIC_ERROR, statementContinue, "");
		}
	}

	@Override
	public void visit(StatementFor statementFor) {
		// ako postoji uslov for petlje, mora biti tipa bool
		if (statementFor.getOptCondition() instanceof OptCondition1) {
			Struct condType = getNodeType(((OptCondition1) statementFor.getOptCondition()).getCondition());
			if (!condType.equals(boolType)) {
				log.error("Greska na liniji " + statementFor.getLine() + ": uslov for mora biti tipa bool");
				report_error(ErrorCode.SEMANTIC_ERROR, statementFor, "");
			}
		}
		loopDepth = Math.max(0, loopDepth - 1);
	}
	
	
	@Override
	public void visit(OptCondition0 optCondition0) { 
		// nema uslova, tretiramo kao da je uslov uvek true

		if (optCondition0.getParent() instanceof StatementFor) {
			loopDepth++;
		}
	}
	
	@Override
	public void visit(OptCondition1 optCondition1) { 
		// postoji uslov, proveravamo ga u StatementFor, a ovde samo povećavamo dubinu petlje
		if (optCondition1.getParent() instanceof StatementFor) {
			loopDepth++;
		}
	}

	@Override
	public void visit(StatementSwitch statementSwitch) {
		// switch izraz mora biti int
		Struct exprType = getNodeType(statementSwitch.getExpr());
		
		if (!isIntLikeType(exprType)) {
			log.error("Greska na liniji " + statementSwitch.getLine() + ": switch izraz mora biti int");
			report_error(ErrorCode.SEMANTIC_ERROR, statementSwitch, "");
		}
		
		switchDepth = Math.max(0, switchDepth - 1);
		
		if (!switchCaseValues.isEmpty()) {
			switchCaseValues.remove(switchCaseValues.size() - 1);
		}
	}

	@Override
	public void visit(CaseList0 caseList0) { 
		// ulazimo u novi switch case, pronalazimo pripadajući StatementSwitch
		// i povećavamo dubinu switch-a
		
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
		// u jednom switch-u svaka case vrednost mora biti jedinstvena
		
		if (switchCaseValues.isEmpty()) {
			return;
		}
		
		Set<Integer> used = switchCaseValues.get(switchCaseValues.size() - 1);
		
		if (used.contains(caseNode.getValue())) {
			log.error("Greska na liniji " + caseNode.getLine() + ": duplirana switch case vrednost " + caseNode.getValue());
			report_error(ErrorCode.SEMANTIC_ERROR, caseNode, "");
		} 
		else {
			used.add(caseNode.getValue());
		}
	}
	
	@Override
	public void visit(StatementIf statementIf) {
		// uslov if mora biti tipa bool
		Struct conditionType = getNodeType(statementIf.getCondition());
		if (!conditionType.equals(boolType)) {
			log.error("Greska na liniji " + statementIf.getLine() + ": uslov if mora biti tipa bool");
			report_error(ErrorCode.SEMANTIC_ERROR, statementIf, "");
		}
	}
	
}
