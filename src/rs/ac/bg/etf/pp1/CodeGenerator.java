package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPC;
	private String currentProgramName;
	private Obj currentProgram = Tab.noObj;
	private Obj currentMethod = Tab.noObj;
	
	// stekovi za čuvanje adresa koje treba naknadno patchovati
	// za break i continue naredbe unutar petlji i switch-a
	private final Stack<List<Integer>> breakfixAddrs = new Stack<>();
	private final Stack<List<Integer>> continuefixAddrs = new Stack<>();

	public int getMainPC() {
		return mainPC;
	}

	public String getProgramName() {
		return currentProgramName;
	}

	public CodeGenerator() {
		initBuiltIns();
	}
	
	// implementacija ugrađenih funkcija ord, chr i len
	private void initBuiltIns() {
		Obj ord = Tab.find("ord");
		Obj chr = Tab.find("chr");
		ord.setAdr(Code.pc);
		chr.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load_n);
		Code.put(Code.exit);
		Code.put(Code.return_);

		Obj len = Tab.find("len");
		len.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load_n);
		Code.put(Code.arraylength);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
		
	// PROGRAM
	@Override
	public void visit(Program program) {
		currentProgramName = program.getProgramName().getName();
		currentProgram = Tab.find(currentProgramName);
		// poziva emitMethodDeclList da bi se sve metode programa emitovale
		emitMethodDeclList(program.getMethodDeclList());
	}
	
	// METHOD
	private void emitMethodDeclList(MethodDeclList list) {
		// rekurzivno prolazi kroz listu metoda i emituje svaku od njih
		if (list instanceof MethodDeclList1) {
			MethodDeclList1 n = (MethodDeclList1) list;
			emitMethodDeclList(n.getMethodDeclList());
			// poziva emitMethod za trenutnu metodu
			emitMethod(n.getMethodDecl());
		}
	}

	private void emitMethod(MethodDecl m) {
		MethodName methodName = m.getMethodName();
		currentMethod = methodName.obj;
		currentMethod.setAdr(Code.pc);
		
		// za main dodatno čuvamo adresu početka
		if ("main".equals(methodName.getName())) {
			mainPC = Code.pc;
		}
		
		Code.put(Code.enter);
		Code.put(currentMethod.getLevel());
		Code.put(currentMethod.getLocalSymbols().size());
		
		// emituje telo metode (sve statemente)
		emitStatementList(m.getStatementList());
		
		if (currentMethod.getType() == Tab.noType) {
			Code.put(Code.exit);
			Code.put(Code.return_);
		} else {
			Code.put(Code.trap); // RUNTIME ERROR
			Code.put(1);
		}
		currentMethod = Tab.noObj;
	}
	
	// STATEMENT
	private void emitStatementList(StatementList list) {
		// rekurzivno prolazi kroz listu statementa i emituje svaki od njih
		if (list instanceof StatementList1) {
			StatementList1 n = (StatementList1) list;
			emitStatementList(n.getStatementList());
			// poziva emitStatement za trenutni statement
			emitStatement(n.getStatement()); 
		}
	}

	private void emitStatement(Statement s) {
		if (s instanceof StatementStmtList) 
			emitStatementList(((StatementStmtList) s).getStatementList());
		
		else if (s instanceof StatementDesignator)
			emitDesignatorStatement(((StatementDesignator) s).getDesignatorStatement());
		
		else if (s instanceof StatementRead)
			emitRead((StatementRead) s);
		
		else if (s instanceof StatementPrint)
			emitPrint((StatementPrint) s);
		
		else if (s instanceof StatementReturn)
			emitReturn((StatementReturn) s);
		
		else if (s instanceof StatementIf)
			emitIf((StatementIf) s);
		
		else if (s instanceof StatementFor)
			emitFor((StatementFor) s);
		
		// break i continue se emituju tako da se na stek stavljaju adrese
		// koje treba naknadno patchovati
		else if (s instanceof StatementBreak) {
			Code.putJump(0);
			breakfixAddrs.peek().add(Code.pc - 2);
		} 
		else if (s instanceof StatementContinue) {
			Code.putJump(0);
			continuefixAddrs.peek().add(Code.pc - 2);
		} 
		else if (s instanceof StatementSwitch)
			emitSwitch((StatementSwitch) s);
	}
	
	
	private void emitReturn(StatementReturn s) {
		// ako metoda vraća vrednost, emitujemo expr koja se vraća
		if (s.getOptExpr() instanceof OptExpr1) {
			emitExpr(((OptExpr1) s.getOptExpr()).getExpr());
		}
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	private void emitIf(StatementIf s) {
		List<Integer> falsefixAddrs = new ArrayList<>();
	
		emitConditionFalseJumps(s.getCondition(), falsefixAddrs); // emitujemo uslov i uradi false fixAddrs
		emitStatement(s.getStatement()); // emitujemo THEN deo
		
		if (s.getOptElse() instanceof OptElse1) { // ako ima ELSE deo
			Code.putJump(0);
			int endJump = Code.pc - 2;
			fixAll(falsefixAddrs); // patchujemo false fixAddrs da idu na ELSE deo
			emitStatement(((OptElse1) s.getOptElse()).getStatement()); // emitujemo ELSE deo
			Code.fixup(endJump);
		} else
			fixAll(falsefixAddrs);
	}

	private void emitFor(StatementFor s) {
		if (s.getOptDesignatorStatement() instanceof OptDesignatorStatement1) // emitujemo inicijalizaciju for petlje (ako postoji)
			emitDesignatorStatement(((OptDesignatorStatement1) s.getOptDesignatorStatement()).getDesignatorStatement());

		int condStart = Code.pc; // zapamti adresu početka uslova
		List<Integer> falsefixAddrs = new ArrayList<>();
		if (s.getOptCondition() instanceof OptCondition1) // emitujemo uslov for petlje i uradi false fixAddrs
			emitConditionFalseJumps(((OptCondition1) s.getOptCondition()).getCondition(), falsefixAddrs);
		
		// pripremamo stekove za break i continue naredbe unutar for petlje
		breakfixAddrs.push(new ArrayList<>());
		continuefixAddrs.push(new ArrayList<>());

		emitStatement(s.getStatement()); // emitujemo telo for petlje

		fixAll(continuefixAddrs.peek());
		if (s.getOptDesignatorStatement1() instanceof OptDesignatorStatement1) { // emitujemo update deo for petlje (ako postoji)
			emitDesignatorStatement(((OptDesignatorStatement1) s.getOptDesignatorStatement1()).getDesignatorStatement());
		}

		Code.putJump(condStart); // bezuslovni skok nazad na proveru uslova
		fixAll(falsefixAddrs);
		fixAll(breakfixAddrs.pop());
		continuefixAddrs.pop();
	}

	private void emitSwitch(StatementSwitch s) {
		emitExpr(s.getExpr()); // emitujemo expr
		
		List<Case> cases = new ArrayList<>();
		collectCases(s.getCaseList(), cases); // sakupljamo sve case-ove
		
		// pripremamo stek za break naredbe unutar switch-a
		breakfixAddrs.push(new ArrayList<>());

		List<Integer> caseJumps = new ArrayList<>();
		for (Case c : cases) {
			// dupliciramo vrh steka (vrednost expr-a) i upoređujemo sa vrednošću case-a
			Code.put(Code.dup);
			Code.loadConst(c.getValue());
			Code.putFalseJump(Code.ne, 0);
			caseJumps.add(Code.pc - 2);
		}
		
		// ako nijedan case nije odgovarao, skačemo na kraj switch-a
		Code.putJump(0);
		int noMatchJump = Code.pc - 2;
		
		// emitujemo svaki case i patchujemo jump-ove da idu na odgovarajuće case-ove
		for (int i = 0; i < cases.size(); i++) {
			Code.fixup(caseJumps.get(i));
			emitStatementList(cases.get(i).getStatementList()); // emitujemo telo case-a
		}
		
		// na kraju switch-a patchujemo noMatchJump da ide na kraj switch-a
		// i break fixAddrs da idu na kraj switch-a
		int switchEnd = Code.pc;
		Code.fixup(noMatchJump);
		fixAll(breakfixAddrs.pop());
		Code.put(Code.pop);
	}
	
	// rekurzivna metoda koja prolazi kroz case-ove i sakuplja ih u listu
	private void collectCases(CaseList list, List<Case> out) {
		if (list instanceof CaseList1) {
			CaseList1 n = (CaseList1) list;
			collectCases(n.getCaseList(), out);
			out.add(n.getCase());
		}
	}
	
	private void emitDesignatorStatement(DesignatorStatement ds) {
		Designator d = ds.getDesignator();
		Obj obj = resolveDesignator(d); // rešavamo designator
		DesignatorStmtTail tail = ds.getDesignatorStmtTail();

		if (tail instanceof DesignatorStmtTail1) { // emitujemo d = expr
			emitStore(d, obj, ((DesignatorStmtTail1) tail).getExpr());
		} else if (tail instanceof DesignatorStmtTail2) { // emitujemo d(...) - poziv funkcije
			emitOptActPars(((DesignatorStmtTail2) tail).getOptActPars());
			emitCall(obj);
			if (obj.getType() != Tab.noType) {
				Code.put(Code.pop);
			}
			// emitujemo d++ ili d--
		} else if (tail instanceof DesignatorStmtTail3 || tail instanceof DesignatorStmtTail4) {
			boolean isInc = tail instanceof DesignatorStmtTail3;
			emitIncDec(d, obj, isInc);
		}
	}
	
	private void emitStore(Designator d, Obj obj, Expr rhs) {
		if (obj.getKind() == Obj.Elem) {
			// za elemente niza, prvo moramo učitati adresu elementa, zatim evaluirati izraz i sačuvati vrednost na tu adresu
			emitArrayElemAddress(d);
			emitExpr(rhs);
			Code.put(obj.getType() == Tab.charType ? Code.bastore : Code.astore);
		} else {
			// za obične promenljive, dovoljno je evaluirati izraz i sačuvati vrednost
			emitExpr(rhs);
			Code.store(obj);
		}
	}

	private void emitIncDec(Designator d, Obj obj, boolean inc) {
		int op = inc ? Code.add : Code.sub;
		if (obj.getKind() == Obj.Elem) {
			// za elemente niza, prvo moramo učitati adresu elementa, zatim njegovu vrednost, modifikovati je i sačuvati nazad
			emitArrayElemAddress(d); // emitujemo adresu elementa niza
			Code.put(Code.dup2); // dupliramo adresu i vrednost elementa na steku
			Code.put(obj.getType() == Tab.charType ? Code.baload : Code.aload);
			Code.loadConst(1); // stavljamo 1 na stek
			Code.put(op); // dodajemo ili oduzimamo 1
			Code.put(obj.getType() == Tab.charType ? Code.bastore : Code.astore);
		} else {
			// za obične promenljive, dovoljno je učitati, modifikovati i sačuvati nazad
			Code.load(obj);
			Code.loadConst(1);
			Code.put(op);
			Code.store(obj);
		}
	}
	
	private void emitRead(StatementRead s) {
		Designator d = s.getDesignator();
		Obj obj = resolveDesignator(d); // razrešavamo deisgnator koji se čita

		if (obj.getKind() == Obj.Elem) {
			// za elemente niza, prvo moramo učitati adresu elementa, zatim pročitati vrednost i sačuvati je na tu adresu
			emitArrayElemAddress(d);
			Code.put(obj.getType() == Tab.charType ? Code.bread : Code.read);
			Code.put(obj.getType() == Tab.charType ? Code.bastore : Code.astore);
		} else {
			// za obične promenljive, dovoljno je pročitati vrednost i sačuvati je
			Code.put(obj.getType() == Tab.charType ? Code.bread : Code.read);
			Code.store(obj);
		}
	}

	private void emitPrint(StatementPrint s) {
		emitExpr(s.getExpr()); // emitujemo izraz koji se štampa
		// određujemo širinu ispisa: ako imamo print(expr, width), širina je data width-om, inače je 0 (podrazumevano)
		int width = (s.getOptNumConst() instanceof OptNumConst1) ? ((OptNumConst1) s.getOptNumConst()).getWidth() : 0;
		boolean isChar = exprType(s.getExpr()) == Tab.charType;
		Code.loadConst(width);
		Code.put(isChar ? Code.bprint : Code.print);
	}
	
	// EXPR
	private void emitExpr(Expr e) {
		if (e instanceof Expr1) { // simpleExpr bez || operatora
			emitSimpleExpr(((Expr1) e).getSimpleExpr());
			return;
		}

		Expr2 t = (Expr2) e; // imamo simpleExpr || expr
		List<Integer> falsefixAddrs = new ArrayList<>();
		emitCondFactFalseJump(t.getCondFact(), falsefixAddrs);
		emitExpr(t.getExpr()); // emitujemo desnu stranu || operatora
		Code.putJump(0);
		int endJump = Code.pc - 2;
		fixAll(falsefixAddrs);
		emitExpr(t.getExpr1()); // emitujemo levu stranu || operatora
		Code.fixup(endJump);
	}

	private void emitSimpleExpr(SimpleExpr se) {
		if (se instanceof SimpleExpr1) { // simpleExpr bez + ili - operatora
			emitTerm(((SimpleExpr1) se).getTerm());
		} else if (se instanceof SimpleExpr2) { // simpleExpr sa unarnim - operatorom
			emitTerm(((SimpleExpr2) se).getTerm());
			Code.put(Code.neg);
		} else { // simpleExpr sa + ili - operatorom
			SimpleExpr3 e = (SimpleExpr3) se;
			emitSimpleExpr(e.getSimpleExpr());
			emitTerm(e.getTerm());
			Code.put(e.getAddop() instanceof AddopADD ? Code.add : Code.sub);
		}
	}
	
	// TERM
	private void emitTerm(Term t) {
		if (t instanceof Term1)  // term bez * ili / operatora
			emitFactor(((Term1) t).getFactor());
		else { // term sa * ili / operatorom
			Term2 e = (Term2) t;
			emitTerm(e.getTerm());
			emitFactor(e.getFactor());
			if (e.getMulop() instanceof MulopMUL)
				Code.put(Code.mul);
			else if (e.getMulop() instanceof MulopDIV)
				Code.put(Code.div);
			else
				Code.put(Code.rem);

		}
	}
	
	// FACTOR
	private void emitFactor(Factor f) {
		if (f instanceof FactorNumConst) //  NUM_CONST
			Code.loadConst(((FactorNumConst) f).getNumValue());
		else if (f instanceof FactorCharConst) // CHAR_CONST
			Code.loadConst(((FactorCharConst) f).getCharValue());
		else if (f instanceof FactorBoolConst) // BOOL_CONST
			Code.loadConst(((FactorBoolConst) f).getBoolValue());
		else if (f instanceof FactorExpr) // EXPR
			emitExpr(((FactorExpr) f).getExpr());
		else if (f instanceof FactorNewArr) { // new int[expr]
			FactorNewArr a = (FactorNewArr) f;
			emitExpr(a.getExpr());
			Code.put(Code.newarray);
			Code.put(resolveType(a.getType()) == Tab.charType ? 0 : 1);
		} else if (f instanceof FactorDesignator) { // designator ili poziv funkcije
			FactorDesignator fd = (FactorDesignator) f;
			Obj obj = resolveDesignator(fd.getDesignator());
			if (fd.getOptCall() instanceof OptCall1) { // poziv funkcije
				emitOptActPars(((OptCall1) fd.getOptCall()).getOptActPars());
				emitCall(obj);
			} else {
				emitLoadDesignator(fd.getDesignator(), obj); // desigantor
			}
		}
	}
	
	// rekurzivna metoda koja emituje stvarne parametre funkcije
	private void emitOptActPars(OptActPars opt) {
		if (opt instanceof OptActPars1) {
			emitExprList(((OptActPars1) opt).getActPars().getExprList());
		}
	}
	
	// rekurzivna metoda koja emituje listu izraza
	private void emitExprList(ExprList list) {
		if (list instanceof ExprList2) { // lista sa više izraza
			ExprList2 n = (ExprList2) list;
			emitExprList(n.getExprList());
			emitExpr(n.getExpr());
		} else { // lista sa jednim izrazom
			emitExpr(((ExprList1) list).getExpr());
		}
	}

	// emitovanje uslova: emituje se uslov i pravi se lista false fixAddr-ova
	// koji se patchuju da idu na mesto gde se nastavlja izvršavanje ako je uslov false
	private void emitConditionFalseJumps(Condition c, List<Integer> falsefixAddrs) {
		List<Integer> truefixAddrs = new ArrayList<>();
		emitConditionJumps(c, truefixAddrs, falsefixAddrs);
		fixAll(truefixAddrs);
	}
	
	// emitovanje uslova unutar || operatora: emituje se uslov i pravi se lista false fixAddr-ova
	private void emitCondFactFalseJump(CondFact cf, List<Integer> falsefixAddrs) {
		emitSimpleExpr(cf.getSimpleExpr());
		if (cf.getOptRel() instanceof OptRel0) {
			Code.loadConst(0);
			Code.putFalseJump(Code.ne, 0); // expr == 0 -> false
		} else {
			OptRel1 rel = (OptRel1) cf.getOptRel();
			emitSimpleExpr(rel.getSimpleExpr());
			Code.putFalseJump(mapRelop(rel.getRelop()), 0); // relacija false
		}
		falsefixAddrs.add(Code.pc - 2);
	}
	
	// emitovanje uslova unutar if i for: emituje se uslov i pravi se lista false fixAddr-ova i lista true fixAddr-ova
	private void emitConditionJumps(Condition c, List<Integer> truefixAddrs, List<Integer> falsefixAddrs) {
		if (c instanceof Condition1) {
			emitCondTermJumps(((Condition1) c).getCondTerm(), truefixAddrs, falsefixAddrs);
		} else {
			Condition2 or = (Condition2) c;
			List<Integer> leftFalse = new ArrayList<>();
			emitConditionJumps(or.getCondition(), truefixAddrs, leftFalse);
			fixAll(leftFalse); // A || B: samo ako je A false, evaluiraj B
			emitCondTermJumps(or.getCondTerm(), truefixAddrs, falsefixAddrs);
		}
	}
	
	// emitovanje uslova unutar if i for
	// emituje se uslov i pravi se lista false fixAddr-ova i lista true fixAddr-ova
	private void emitCondTermJumps(CondTerm t, List<Integer> truefixAddrs, List<Integer> falsefixAddrs) {
		if (t instanceof CondTerm1) {
			emitCondFactJumps(((CondTerm1) t).getCondFact(), truefixAddrs, falsefixAddrs);
		} else {
			CondTerm2 and = (CondTerm2) t;
			List<Integer> leftTrue = new ArrayList<>();
			emitCondTermJumps(and.getCondTerm(), leftTrue, falsefixAddrs);
			fixAll(leftTrue); // A && B: samo ako je A true, evaluiraj B
			emitCondFactJumps(and.getCondFact(), truefixAddrs, falsefixAddrs);
		}
	}
	
	// emitovanje uslova unutar if i for
	// emituje se uslov i pravi se lista false fixAddr-ova i lista true fixAddr-ova
	private void emitCondFactJumps(CondFact cf, List<Integer> truefixAddrs, List<Integer> falsefixAddrs) {
		emitSimpleExpr(cf.getSimpleExpr());
		if (cf.getOptRel() instanceof OptRel0) {
			Code.loadConst(0);
			Code.putFalseJump(Code.ne, 0); // expr == 0 => false
		} else {
			OptRel1 rel = (OptRel1) cf.getOptRel();
			emitSimpleExpr(rel.getSimpleExpr());
			Code.putFalseJump(mapRelop(rel.getRelop()), 0); // relacija false
		}
		falsefixAddrs.add(Code.pc - 2);
		Code.putJump(0); // true
		truefixAddrs.add(Code.pc - 2);
	}
	
	private void emitLoadDesignator(Designator d, Obj obj) {
		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(d.getDesignatorSuffixList(), suffixes);
		if (suffixes.isEmpty()) {
			Code.load(obj);
			return;
		}

		DesignatorSuffix last = suffixes.get(suffixes.size() - 1);
		Obj base = findSymbol(d.getName());
		if (last instanceof DesignatorSuffix2) {
			Code.load(base);
			Code.put(Code.arraylength);
		} else if (last instanceof DesignatorSuffix3) {
			emitArrayElemAddress(d);
			Code.put(obj.getType() == Tab.charType ? Code.baload : Code.aload);
		} else {
			Code.load(obj);
		}
	}
	
	// emitovanje adrese elementa niza
	// emituje se adresa elementa niza na stek
	private void emitArrayElemAddress(Designator d) {
		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(d.getDesignatorSuffixList(), suffixes);
		// dohvatamo poslednji sufiks
		DesignatorSuffix3 idx = (DesignatorSuffix3) suffixes.get(suffixes.size() - 1);
		Code.load(findSymbol(d.getName()));
		emitExpr(idx.getExpr());
	}
	
	// emitovanje poziva funkcije
	private void emitCall(Obj methodObj) {
		String name = methodObj.getName();
		// ugradjene metode
		if ("ord".equals(name) || "chr".equals(name)) {
			return;
		}
		if ("len".equals(name)) {
			Code.put(Code.arraylength);
			return;
		}
		// ostale metode
		int offset = methodObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}
	
	// POMOĆNE FUNKCIJE
	
	private int mapRelop(Relop relop) {
		if (relop instanceof RelopEQ) return Code.eq;
		if (relop instanceof RelopNEQ) return Code.ne;
		if (relop instanceof RelopGT) return Code.gt;
		if (relop instanceof RelopGE) return Code.ge;
		if (relop instanceof RelopLT) return Code.lt;
		return Code.le;
	}
	
	private void fixAll(List<Integer> addrs) {
		for (Integer a : addrs) {
			Code.fixup(a);
		}
		addrs.clear(); // skidamo sa addrenog steka nakon patchovanja
	}

	private Obj findSymbol(String name) {
		if (currentMethod != null && currentMethod != Tab.noObj) {
			for (Obj o : currentMethod.getLocalSymbols()) {
				if (name.equals(o.getName())) {
					return o;
				}
			}
		}
		if (currentProgram != null && currentProgram != Tab.noObj) {
			for (Obj o : currentProgram.getLocalSymbols()) {
				if (name.equals(o.getName())) {
					return o;
				}
			}
		}
		return Tab.find(name);
	}

	private Struct resolveType(Type t) {
		Obj typeObj = findSymbol(t.getType());
		return typeObj != Tab.noObj && typeObj.getKind() == Obj.Type ? typeObj.getType() : Tab.noType;
	}
	
	private void collectSuffixes(DesignatorSuffixList list, List<DesignatorSuffix> out) {
		if (list instanceof DesignatorSuffixList1) {
			DesignatorSuffixList1 n = (DesignatorSuffixList1) list;
			collectSuffixes(n.getDesignatorSuffixList(), out);
			out.add(n.getDesignatorSuffix());
		}
	}

	private Obj resolveDesignator(Designator d) {
		Obj obj = findSymbol(d.getName());
		if (obj == Tab.noObj) return obj;

		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(d.getDesignatorSuffixList(), suffixes);

		for (DesignatorSuffix s : suffixes) {
			if (s instanceof DesignatorSuffix3) {
				obj = new Obj(Obj.Elem, "$elem", obj.getType().getElemType());
			} else if (s instanceof DesignatorSuffix2) {
				obj = new Obj(Obj.Fld, "length", Tab.intType);
			} else if (s instanceof DesignatorSuffix1) {
				String name = ((DesignatorSuffix1) s).getField();
				Obj member = Tab.noObj;
				if (obj.getKind() == Obj.Type && obj.getType().getKind() == Struct.Enum) {
					for (Obj o : obj.getLocalSymbols()) {
						if (name.equals(o.getName())) {
							member = o;
							break;
						}
					}
				}
				obj = member;
			}
		}
		return obj;
	}

	private Struct exprType(Expr e) {
		if (e instanceof Expr1) {
			return simpleExprType(((Expr1) e).getSimpleExpr());
		}
		return exprType(((Expr2) e).getExpr());
	}

	private Struct simpleExprType(SimpleExpr se) {
		if (se instanceof SimpleExpr1) return termType(((SimpleExpr1) se).getTerm());
		if (se instanceof SimpleExpr2) return Tab.intType;
		return Tab.intType;
	}

	private Struct termType(Term t) {
		if (t instanceof Term1) return factorType(((Term1) t).getFactor());
		return Tab.intType;
	}

	private Struct factorType(Factor f) {
		if (f instanceof FactorCharConst) return Tab.charType;
		if (f instanceof FactorBoolConst) {
			Obj boolObj = Tab.find("bool");
			return boolObj == Tab.noObj ? Tab.noType : boolObj.getType();
		}
		if (f instanceof FactorDesignator) return resolveDesignator(((FactorDesignator) f).getDesignator()).getType();
		if (f instanceof FactorExpr) return exprType(((FactorExpr) f).getExpr());
		return Tab.intType;
	}
}
