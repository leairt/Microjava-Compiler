package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPC;
	private String currentProgramName;

	private Obj currentProgram;
	private Obj currentMethod;


	// KONTEKST ZA IF/ELSE

	// za svaki StatementIf čuvamo listu skokova koji treba da vode na false granu,
	// tj. ili na else ili iza if-a ako nema else dela
	private final Map<StatementIf, List<Integer>> ifFalseJumps = new HashMap<>();

	// ako if ima else, onda na kraju if dela treba da se skoči iza else dela,
	// zato čuvamo adresu tog skoka ovde dok ne postane poznat cilj
	private final Map<StatementIf, Integer> ifEndJumps = new HashMap<>();


	// KONTEKST ZA FOR PETLJU

	// adrese koje se popunjavaju naknadno kad postane poznat cilj skoka za for petlju
	private static class LoopContext {
		StatementFor node;

		int condStart = -1;   // početak provere uslova (posle inicilazacije)
		int bodyJump = -1;    // skok na telo petlje
		int falseJump = -1;   // izlazak iz petlje (ako uslov nije zadovoljen)
		int updateStart = -1; // početak update dela (++, --,...)

		// skokovi koji vode na izlazak iz petlje (break) i na update deo (continue)
		// čuvaju se u listama jer se može desiti da se break/continue "propadnu" kroz više nivoa ugnježdenosti
		List<Integer> breakJumps = new ArrayList<>();
		List<Integer> continueJumps = new ArrayList<>();
	}

	private final Map<StatementFor, LoopContext> forContexts = new HashMap<>();
	private final List<LoopContext> loopStack = new ArrayList<>();


	// KONTEKST ZA SWITCH

	// break iz switch-a se patchuje tek kad se zatvori ceo switch
	private static class SwitchContext {
		StatementSwitch node;

		// skokovi koji vode na izlazak iz switch-a (break)
		// čuvaju se u listi jer se može desiti da se break "propadne" kroz više nivoa ugnježdenosti
		List<Integer> breakJumps = new ArrayList<>();
	}

	private final List<SwitchContext> switchStack = new ArrayList<>();


	// SUPPRESSION i FORCE EMIT

	// skup čvorova koji su koreni podstabala koja treba ignorisati tokom obilaska
	private final Set<SyntaxNode> suppressedRoots = new HashSet<>();

	// gasimo suppressovanje u posebnim slučajevima kada želimo ručno da obilazimo neko podstablo
	private boolean forceEmit = false;

	
	// TRUE/FALSE PATCH LISTE ZA USLOVE

	private static class BoolLists {
		// skokovi koji vode na true, odnosno na false granu uslova
		List<Integer> trueJumps = new ArrayList<>();
		List<Integer> falseJumps = new ArrayList<>();
	}

	// getteri
	public int getMainPC() {
		return this.mainPC;
	}

	public String getProgramName() {
		return this.currentProgramName;
	}

	// POMOĆNE FUNKCIJE
	
	// pretvara AST relacioni operator u MJVM instrukcije
	private int mapRelop(Relop relop) {
		if (relop instanceof RelopEQ)  return Code.eq;
		if (relop instanceof RelopNEQ) return Code.ne;
		if (relop instanceof RelopGT)  return Code.gt;
		if (relop instanceof RelopGE)  return Code.ge;
		if (relop instanceof RelopLT)  return Code.lt;
		return Code.le;
	}
	
	// funkcija za fixup-ovanje skokova sa 0 na poznatu adresu
	private void fixJumps(List<Integer> jumps, int targetPc) {
		for (Integer adr : jumps) {
			// nije MJVM instrukcija, već ručno fix-ovanje relativnog offseta
			Code.put2(adr, targetPc - adr + 1);
			// napomena: pošto se radi o relativnom skoku
			//           a targetPc je adresa prve instrukcije nakon skoka
			//           offset je targetPc - adr + 1
		}
	}
	
	// proverava da li je dati čvor unutar nekog od potstabala koje treba preskočiti tokom generisanja koda
	private boolean isSuppressed(SyntaxNode node) {
		if (forceEmit || node == null) return false;

		SyntaxNode currentNode = node;
		while (currentNode != null) {
			if (suppressedRoots.contains(currentNode)) return true;
			currentNode = currentNode.getParent();
		}

		boolean inCase = false;
		currentNode = node;
		while (currentNode != null) {
			if (currentNode instanceof Case) inCase = true;
			if (currentNode instanceof StatementSwitch) {
				if (inCase) return true;
				break;
			}
			currentNode = currentNode.getParent();
		}
		return false;
	}
	
	// proverava da li je dati čvor unutar if uslova tj. if (OVDE)
	private boolean isInsideIfCondition(SyntaxNode node) {
		SyntaxNode currentNode = node;
		SyntaxNode condRoot = null;

		while (currentNode != null) {
			if (currentNode instanceof Condition1
					|| currentNode instanceof Condition2) {
				condRoot = currentNode;
			}
			if (currentNode instanceof StatementIf) {
				StatementIf si = (StatementIf) currentNode;
				return condRoot != null && si.getCondition() == condRoot;
			}
			currentNode = currentNode.getParent();
		}
		return false;
	}
	
	// funkcija koja se poziva na kraju then grane if-a
	// da bi se fixup-ovali skokovi koji su čekali da se zna adresa kraja bloka
	private void afterThenStatement(Statement statementNode) {
		SyntaxNode p = statementNode.getParent();
		if (!(p instanceof StatementIf)) return;

		StatementIf ifNode = (StatementIf) p;
		if (ifNode.getStatement() != statementNode) return;
		if (!(ifNode.getOptElse() instanceof OptElse1)) return;

		// 42 - jmp s
		Code.putJump(0);
		ifEndJumps.put(ifNode, Code.pc - 2);

		List<Integer> falseJumps = ifFalseJumps.remove(ifNode);
		if (falseJumps != null) {
			fixJumps(falseJumps, Code.pc);
		}
	}
	
	// funkcija koja rekurzivno sakuplja sve case grane unutar switch-a
	private void collectCases(CaseList list, List<Case> out) {
		if (list instanceof CaseList1) {
			CaseList1 l = (CaseList1) list;
			collectCases(l.getCaseList(), out);
			out.add(l.getCase());
		}
	}
	
	// funkcija koja rekurzivno obilazi listu naredbi(statement-a) i generiše kod za svaku od njih
	private void emitStatementList(StatementList list) {
		boolean old = forceEmit;
		forceEmit = true; // force emit da bi se generisao kod za sve statemente
		try {
			if (list != null) list.traverseBottomUp(this);
		} finally {
			forceEmit = old;
		}
	}
	
	// funkcija koja dohvata Struct koji odgovara tipu Type datom u AST-u
	private Struct resolveTypeSymbol(Type typeNode) {
		Obj typeObj = findSymbol(typeNode.getType());
		if (typeObj != Tab.noObj && typeObj.getKind() == Obj.Type) {
			return typeObj.getType();
		}
		return Tab.noType;
	}
	
	// funkcija koja dohvata Obj koji odgovara datom imenu
	// prvo proverava lokalne simbole trenutne metode, zatim lokalne simbole programa, i na kraju globalne simbole
	private Obj findSymbol(String name) {
		if (currentMethod != null) {
			for (Obj symbol : currentMethod.getLocalSymbols()) {
				if (symbol.getName().equals(name)) {
					return symbol;
				}
			}
		}

		if (currentProgram != null) {
			for (Obj symbol : currentProgram.getLocalSymbols()) {
				if (symbol.getName().equals(name)) {
					return symbol;
				}
			}
		}

		return Tab.find(name);
	}

	// funkcija koja rekurzivno sakuplja sve sufikse designatora u jednu listu
	private void collectSuffixes(DesignatorSuffixList suffixList, List<DesignatorSuffix> out) {
		if (suffixList instanceof DesignatorSuffixList1) {
			DesignatorSuffixList1 list1 = (DesignatorSuffixList1) suffixList;
			collectSuffixes(list1.getDesignatorSuffixList(), out);
			out.add(list1.getDesignatorSuffix());
		}
	}
	
	// funkcija koja dohvata poslednji indeksni sufiks iz designatora
	private DesignatorSuffix3 getLastIndexSuffix(Designator designator) {
		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(designator.getDesignatorSuffixList(), suffixes);
		if (suffixes.isEmpty()) {
			return null;
		}
		DesignatorSuffix last = suffixes.get(suffixes.size() - 1);
		return last instanceof DesignatorSuffix3 ? (DesignatorSuffix3) last : null;
	}

	// funkcija koja dohvata odgovarajući Obj za zadati designator Designator u AST-u, uzimajući u obzir sve sufikse
	private Obj resolveDesignatorObj(Designator designator) {
		Obj currentObj = findSymbol(designator.getName());
		if (currentObj == Tab.noObj) {
			return Tab.noObj;
		}

		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(designator.getDesignatorSuffixList(), suffixes);

		for (DesignatorSuffix suffix : suffixes) {
			if (suffix instanceof DesignatorSuffix3) {
				if (currentObj.getType().getKind() != Struct.Array) {
					return Tab.noObj;
				}
				currentObj = new Obj(Obj.Elem, "$elem", currentObj.getType().getElemType());
				continue;
			}

			if (suffix instanceof DesignatorSuffix2) {
				currentObj = new Obj(Obj.Fld, "length", Tab.intType);
				continue;
			}

			if (suffix instanceof DesignatorSuffix1) {
				String memberName = ((DesignatorSuffix1) suffix).getField();

				if (currentObj.getKind() == Obj.Type && currentObj.getType().getKind() == Struct.Enum) {
					Obj found = Tab.noObj;
					for (Obj member : currentObj.getLocalSymbols()) {
						if (member.getName().equals(memberName)) {
							found = member;
							break;
						}
					}
					currentObj = found;
					continue;
				}

				return Tab.noObj;
			}
		}

		return currentObj;
	}
	
	// funkcija koja dohvata odgovarajući Struct za zadati designator Designator u AST-u
	private Struct resolveDesignatorType(Designator designator) {
		Obj obj = findSymbol(designator.getName());
		if (obj == Tab.noObj) {
			return Tab.noType;
		}

		Struct currentType = obj.getType();
		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(designator.getDesignatorSuffixList(), suffixes);

		for (DesignatorSuffix suffix : suffixes) {
			if (currentType == null) {
				return Tab.noType;
			}

			if (suffix instanceof DesignatorSuffix3) {
				if (currentType.getKind() != Struct.Array) {
					return Tab.noType;
				}
				currentType = currentType.getElemType();
				continue;
			}

			if (suffix instanceof DesignatorSuffix2) {
				currentType = Tab.intType;
				continue;
			}

			if (suffix instanceof DesignatorSuffix1) {
				currentType = Tab.intType;
			}
		}

		return currentType;
	}

	// funkcija koja dohvata vrednost designatora, uzimajući u obzir sve sufikse i stavlja je na stek
	// sufiksi se rayrešavaju s desna na levo, tj. prvo poslednji, pa pretposlednji, itd.
	private void emitLoadDesignatorValue(Designator designator) {
		Obj baseObj = findSymbol(designator.getName()); // osnovni designator bez sufiksa
		
		if (baseObj == Tab.noObj) {
			return;
		}

		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(designator.getDesignatorSuffixList(), suffixes);

		if (suffixes.isEmpty()) { // nema sufiksa
			// load designatora
			Code.load(baseObj); // učitava se osnovna vrednost designatora
			return;
		}

		DesignatorSuffix last = suffixes.get(suffixes.size() - 1);

		if (last instanceof DesignatorSuffix2) { // length sufiks
			Code.load(baseObj); // učitava se cela vrednost niza
			// 38 - arraylength
			Code.put(Code.arraylength); // na stek se stavlja dužina niza
			return;
		}

		if (last instanceof DesignatorSuffix1) { // field sufiks
			Obj resolved = resolveDesignatorObj(designator);
			
			if (resolved != Tab.noObj) { 
				// load resolved designatora
				Code.load(resolved); // učitava se vrednost razrešenog designatora
			}
			return;
		}

		// DesignatorSuffix3 - element niza
		
		// 39 - pop
		Code.put(Code.pop);
		// uklanjamo već izračunat indeks sa steka koji se generisao tokom AST obilaska
		// regenerišemo ga ručno da bismo mogli da kontrolišemo redosled elemenata na steku


		DesignatorSuffix3 indexSuffix = getLastIndexSuffix(designator);
		if (indexSuffix == null) {
			return;
		}

		// load osnovnog designatora (niza)
		Code.load(baseObj);
		indexSuffix.getExpr().traverseBottomUp(this); // generiše kod za indeks

		Struct elemType = baseObj.getType().getElemType();
		// 34 - aload / 36 - baload
		Code.put(elemType == Tab.charType ? Code.baload : Code.aload);
		// na stek se stavlja element niza na poziciji datoj indeksom
		// aload se koristi za int i bool nizove, baload za char nizove
	}
	
	// vraća da li je dati designator tipa char, uzimajući u obzir sve sufikse
	private boolean isCharDesignator(Designator designator) {
		Struct type = resolveDesignatorType(designator);
		return type == Tab.charType;
	}
	
	// vraća da li je dati factor tipa char
	private boolean isCharFactor(Factor factor) {
		if (factor instanceof FactorCharConst) { // char literal
			return true;
		}

		if (factor instanceof FactorExpr) { // izraz u zagradama
			return isCharExpression(((FactorExpr) factor).getExpr());
		}

		if (factor instanceof FactorDesignator) { // designator
			FactorDesignator factorDesignator = (FactorDesignator) factor;
			if (factorDesignator.getOptCall() instanceof OptCall0) {
				return isCharDesignator(factorDesignator.getDesignator());
			}
		}

		return false;
	}
	
	// vraća da li je dati term tipa char
	private boolean isCharTerm(Term term) {
		if (term instanceof Term1) {
			return isCharFactor(((Term1) term).getFactor());
		}
		return false;
	}
	
	// vraća da li je dati simpleExpr tipa char
	private boolean isCharSimpleExpr(SimpleExpr simpleExpr) {
		if (simpleExpr instanceof SimpleExpr1) {
			return isCharTerm(((SimpleExpr1) simpleExpr).getTerm());
		}
		return false;
	}
	
	// vraća da li je dati izraz tipa char
	private boolean isCharExpression(Expr expr) {
		if (expr instanceof Expr1) {
			return isCharSimpleExpr(((Expr1) expr).getSimpleExpr());
		}
		if (expr instanceof Expr2) {
			Expr2 ternary = (Expr2) expr;
			return isCharExpression(ternary.getExpr()) && isCharExpression(ternary.getExpr1());
		}
		return false;
	}
	
	// funkcija koja generiše kod za poziv funkcije datog designatora, uzimajući u obzir sve sufikse
	private void emitFunctionCall(Designator designator) {
		Obj funcObj = resolveDesignatorObj(designator);
		
		if (funcObj == Tab.noObj || funcObj.getKind() != Obj.Meth) {
			return;
		}

		String name = funcObj.getName();

		if ("ord".equals(name) || "chr".equals(name)) {
			return;
		}

		if ("len".equals(name)) {
			// 38 - arraylength
			Code.put(Code.arraylength);
			return;
		}

		int offset = funcObj.getAdr() - Code.pc;
		// 49 - call s
		Code.put(Code.call);
		Code.put2(offset);
	}

	// funkcija koja generiše kod za uslovne izraze 
	// vraća liste skokova koje treba fixup-ovati na true i false granu
	private BoolLists emitCondFactLists(CondFact cf) {
		BoolLists bl = new BoolLists();
		
		boolean old = forceEmit;
		forceEmit = true; // radi se o specijalnom slučaju gde želimo da generišemo kod ručno

		try {
			if (cf.getOptRel() instanceof OptRel1) {
				OptRel1 rel = (OptRel1) cf.getOptRel();

				cf.getSimpleExpr().traverseBottomUp(this);
				rel.getSimpleExpr().traverseBottomUp(this);

				int op = mapRelop(rel.getRelop());

				// 43..48 - j<cond> s
				Code.putFalseJump(op, 0);
				bl.falseJumps.add(Code.pc - 2);

				// 42 - jmp s
				Code.putJump(0);
				bl.trueJumps.add(Code.pc - 2);
			} else {
				cf.getSimpleExpr().traverseBottomUp(this);

				// 15..22 - const_* / const w
				Code.loadConst(0);

				// 43..48 - j<cond> s  (ovde proveravamo expr != 0)
				Code.putFalseJump(Code.ne, 0);
				bl.falseJumps.add(Code.pc - 2);

				// 42 - jmp s
				Code.putJump(0);
				bl.trueJumps.add(Code.pc - 2);
			}
		} finally {
			forceEmit = old;
		}

		return bl;
	}

	private BoolLists emitCondTermLists(SyntaxNode termNode) {
		if (termNode instanceof rs.ac.bg.etf.pp1.ast.CondTerm1) {
			return emitCondFactLists(((rs.ac.bg.etf.pp1.ast.CondTerm1) termNode).getCondFact());
		}

		rs.ac.bg.etf.pp1.ast.CondTerm2 t2 = (rs.ac.bg.etf.pp1.ast.CondTerm2) termNode;

		BoolLists left = emitCondTermLists(t2.getCondTerm());
		fixJumps(left.trueJumps, Code.pc);

		BoolLists right = emitCondFactLists(t2.getCondFact());

		BoolLists out = new BoolLists();
		out.trueJumps.addAll(right.trueJumps);
		out.falseJumps.addAll(left.falseJumps);
		out.falseJumps.addAll(right.falseJumps);

		return out;
	}

	private BoolLists emitConditionLists(SyntaxNode conditionNode) {
		if (conditionNode instanceof rs.ac.bg.etf.pp1.ast.Condition1) {
			return emitCondTermLists(((rs.ac.bg.etf.pp1.ast.Condition1) conditionNode).getCondTerm());
		}

		rs.ac.bg.etf.pp1.ast.Condition2 c2 = (rs.ac.bg.etf.pp1.ast.Condition2) conditionNode;

		BoolLists left = emitConditionLists(c2.getCondition());
		fixJumps(left.falseJumps, Code.pc);

		BoolLists right = emitCondTermLists(c2.getCondTerm());

		BoolLists out = new BoolLists();
		out.trueJumps.addAll(left.trueJumps);
		out.trueJumps.addAll(right.trueJumps);
		out.falseJumps.addAll(right.falseJumps);

		return out;
	}

	private void handleIfCondition(SyntaxNode conditionNode) {
		if (isSuppressed(conditionNode)) return;

		if (conditionNode.getParent() instanceof StatementIf) {
			BoolLists lists = emitConditionLists(conditionNode);
			fixJumps(lists.trueJumps, Code.pc);
			ifFalseJumps.put((StatementIf) conditionNode.getParent(), lists.falseJumps);
		}
	}

	// ============================================================
	// POMOĆNE FUNKCIJE - FOR
	// ============================================================

	private void handleOptDesignatorStatement(OptDesignatorStatement optDesignatorStatement) {
		if (isSuppressed(optDesignatorStatement)) return;
		if (!(optDesignatorStatement.getParent() instanceof StatementFor)) return;

		StatementFor forNode = (StatementFor) optDesignatorStatement.getParent();

		LoopContext ctx = forContexts.computeIfAbsent(forNode, n -> {
			LoopContext c = new LoopContext();
			c.node = n;
			return c;
		});

		if (forNode.getOptDesignatorStatement() == optDesignatorStatement) {
			ctx.condStart = Code.pc;
			return;
		}

		if (forNode.getOptDesignatorStatement1() == optDesignatorStatement) {
			if (optDesignatorStatement instanceof OptDesignatorStatement1) {
				// 42 - jmp s
				Code.putJump(ctx.condStart);
			} else {
				ctx.updateStart = ctx.condStart;
			}

			if (ctx.bodyJump >= 0) {
				Code.fixup(ctx.bodyJump);
				ctx.bodyJump = -1;
			}

			if (!loopStack.contains(ctx)) {
				loopStack.add(ctx);
			}
		}
	}

	// ============================================================
	// OVERRIDE-OVANE VISIT METODE
	// ============================================================

	// ------------------------------------------------------------
	// PROGRAM
	// ------------------------------------------------------------

	@Override
	public void visit(ProgramName programName) {
		this.currentProgramName = programName.getName();
		this.currentProgram = Tab.find(programName.getName());
	}

	// ------------------------------------------------------------
	// METHOD
	// ------------------------------------------------------------

	@Override
	public void visit(MethodName methodName) {
		// na početku metode: setujemo adresu i generišemo enter instrukciju
		if ("main".equals(methodName.getName())) {
			mainPC = Code.pc;
		}

		currentMethod = methodName.obj;
		currentMethod.setAdr(Code.pc);

		// 51 - enter b1, b2
		Code.put(Code.enter);
		Code.put(methodName.obj.getLevel());              // b1 = broj formalnih parametara
		Code.put(methodName.obj.getLocalSymbols().size()); // b2 = broj lokalnih simbola
	}

	@Override
	public void visit(MethodDecl methodDecl) {
		// RUNTIME ERROR - ako je ne-void metoda bez return-a, u runtime-u aktiviramo trap(1)
		if (currentMethod != null && currentMethod.getType() != Tab.noType) {
			// 57 - trap b
			Code.put(Code.trap);
			Code.put(1); // b = 1 (runtime error: nedostaje return)
		}

		// na kraju metode generišemo exit i return instrukcije
		// 52 - exit
		Code.put(Code.exit);
		// 50 - return
		Code.put(Code.return_);

		currentMethod = null;
	}

	// ------------------------------------------------------------
	// IF / CONDITION
	// ------------------------------------------------------------

	@Override
	public void visit(rs.ac.bg.etf.pp1.ast.Condition1 condition1) {
		handleIfCondition(condition1);
	}

	@Override
	public void visit(rs.ac.bg.etf.pp1.ast.Condition2 condition2) {
		handleIfCondition(condition2);
	}

	@Override
	public void visit(StatementIf statementIf) {
		// Na kraju if/else patchujemo skok "iza else dela".
		if (isSuppressed(statementIf)) return;

		Integer endJump = ifEndJumps.remove(statementIf);
		if (endJump != null) {
			Code.fixup(endJump);
			afterThenStatement(statementIf);
			return;
		}

		List<Integer> falseJumps = ifFalseJumps.remove(statementIf);
		if (falseJumps != null) {
			fixJumps(falseJumps, Code.pc);
		}

		afterThenStatement(statementIf);
	}

	@Override
	public void visit(CondFact condFact) {
		if (isInsideIfCondition(condFact) && !forceEmit) return;
		if (isSuppressed(condFact)) return;
		if (condFact.getOptRel() instanceof OptRel0) {
			return;
		}

		OptRel1 optRel1 = (OptRel1) condFact.getOptRel();
		int relop = mapRelop(optRel1.getRelop());

		// 43..48 - j<cond> s  (ovde false jump)
		Code.putFalseJump(relop, 0);
		int falseJump = Code.pc - 2;

		// 15..22 - const_* / const w
		Code.loadConst(1);

		// 42 - jmp s
		Code.putJump(0);
		int endJump = Code.pc - 2;

		Code.fixup(falseJump);

		// 15..22 - const_* / const w
		Code.loadConst(0);

		Code.fixup(endJump);
	}

	// ------------------------------------------------------------
	// FOR
	// ------------------------------------------------------------

	@Override
	public void visit(OptDesignatorStatement0 optDesignatorStatement0) {
		handleOptDesignatorStatement(optDesignatorStatement0);
	}

	@Override
	public void visit(OptDesignatorStatement1 optDesignatorStatement1) {
		handleOptDesignatorStatement(optDesignatorStatement1);
	}

	@Override
	public void visit(OptCondition1 optCondition1) {
		if (isSuppressed(optCondition1)) return;
		if (!(optCondition1.getParent() instanceof StatementFor)) return;

		StatementFor forNode = (StatementFor) optCondition1.getParent();

		LoopContext ctx = forContexts.computeIfAbsent(forNode, n -> {
			LoopContext c = new LoopContext();
			c.node = n;
			return c;
		});

		// 15..22 - const_* / const w
		Code.loadConst(0);

		// 43..48 - j<cond> s  (ovde false jump za cond != 0)
		Code.putFalseJump(Code.ne, 0);
		ctx.falseJump = Code.pc - 2;

		// 42 - jmp s
		Code.putJump(0);
		ctx.bodyJump = Code.pc - 2;

		ctx.updateStart = Code.pc;
	}

	@Override
	public void visit(OptCondition0 optCondition0) {
		if (isSuppressed(optCondition0)) return;
		if (!(optCondition0.getParent() instanceof StatementFor)) return;

		StatementFor forNode = (StatementFor) optCondition0.getParent();

		LoopContext ctx = forContexts.computeIfAbsent(forNode, n -> {
			LoopContext c = new LoopContext();
			c.node = n;
			return c;
		});

		// 42 - jmp s
		Code.putJump(0);
		ctx.bodyJump = Code.pc - 2;
		ctx.updateStart = Code.pc;
	}

	@Override
	public void visit(StatementFor statementFor) {
		// Zavrsni patch for petlje: false/uslov, break i continue skokovi.
		if (isSuppressed(statementFor)) return;

		LoopContext ctx = forContexts.remove(statementFor);
		if (ctx == null) return;

		loopStack.remove(ctx);

		if (ctx.updateStart < 0) {
			ctx.updateStart = ctx.condStart;
		}

		// 42 - jmp s
		Code.putJump(ctx.updateStart);

		if (ctx.falseJump >= 0) Code.fixup(ctx.falseJump);
		for (Integer adr : ctx.breakJumps) Code.fixup(adr);
		for (Integer adr : ctx.continueJumps) Code.put2(adr, ctx.updateStart - adr + 1);

		afterThenStatement(statementFor);
	}

	@Override
	public void visit(StatementBreak statementBreak) {
		// break se ne zna odmah gde ide; zapisujemo patch mesto u kontekst.
		if (isSuppressed(statementBreak)) return;

		if (!switchStack.isEmpty()) {
			// 42 - jmp s
			Code.putJump(0);
			switchStack.get(switchStack.size() - 1).breakJumps.add(Code.pc - 2);
			afterThenStatement(statementBreak);
			return;
		}

		if (!loopStack.isEmpty()) {
			// 42 - jmp s
			Code.putJump(0);
			loopStack.get(loopStack.size() - 1).breakJumps.add(Code.pc - 2);
		}

		afterThenStatement(statementBreak);
	}

	@Override
	public void visit(StatementContinue statementContinue) {
		// continue treba da vrati tok na update/cond deo tekuce for petlje.
		if (isSuppressed(statementContinue)) return;

		if (!loopStack.isEmpty()) {
			LoopContext ctx = loopStack.get(loopStack.size() - 1);
			if (ctx.updateStart >= 0) {
				// 42 - jmp s
				Code.putJump(ctx.updateStart);
			} else {
				// 42 - jmp s
				Code.putJump(0);
				ctx.continueJumps.add(Code.pc - 2);
			}
		}

		afterThenStatement(statementContinue);
	}

	// ------------------------------------------------------------
	// SWITCH
	// ------------------------------------------------------------

	@Override
	public void visit(Expr expr) {
		if (isSuppressed(expr)) return;

		if (expr.getParent() instanceof StatementSwitch) {
			StatementSwitch sw = (StatementSwitch) expr.getParent();
			if (sw.getExpr() == expr) {
				suppressedRoots.add(sw.getCaseList());
			}
		}
	}

	@Override
	public void visit(StatementSwitch statementSwitch) {
		// Na kraju switch bloka patchujemo sve break skokove.
		if (isSuppressed(statementSwitch)) return;

		// Ukloni vec izracunatu vrednost switch izraza i ručno generiši case grananje.
		// 39 - pop
		Code.put(Code.pop);

		List<Case> cases = new ArrayList<>();
		collectCases(statementSwitch.getCaseList(), cases);

		List<Integer> caseBodyJumps = new ArrayList<>();

		SwitchContext sw = new SwitchContext();
		sw.node = statementSwitch;
		switchStack.add(sw);

		for (Case c : cases) {
			statementSwitch.getExpr().traverseBottomUp(this);

			// 15..22 - const_* / const w
			Code.loadConst(c.getValue());

			// 43..48 - j<cond> s
			Code.putFalseJump(Code.eq, 0);
			int notEq = Code.pc - 2;

			// 42 - jmp s
			Code.putJump(0);
			caseBodyJumps.add(Code.pc - 2);

			Code.fixup(notEq);
		}

		// 42 - jmp s
		Code.putJump(0);
		int endNoMatch = Code.pc - 2;

		for (int i = 0; i < cases.size(); i++) {
			Code.fixup(caseBodyJumps.get(i));
			emitStatementList(cases.get(i).getStatementList());
		}

		Code.fixup(endNoMatch);

		for (Integer adr : sw.breakJumps) {
			Code.fixup(adr);
		}

		switchStack.remove(switchStack.size() - 1);
		suppressedRoots.remove(statementSwitch.getCaseList());

		afterThenStatement(statementSwitch);
	}

	// ------------------------------------------------------------
	// READ / PRINT / RETURN / WRAPPERI
	// ------------------------------------------------------------

	@Override
	public void visit(StatementPrint statementPrint) {
		// print/bprint traze na steku: value, pa width
		// print je za int i bool, bprint je za char

		if (isSuppressed(statementPrint)) return;

		boolean isBytePrint = isCharExpression(statementPrint.getExpr());
		int width = isBytePrint ? 1 : 0;

		if (statementPrint.getOptNumConst() instanceof OptNumConst1) {
			width = ((OptNumConst1) statementPrint.getOptNumConst()).getWidth();
		}

		// 15..22 - const_* / const w
		Code.loadConst(width);

		// 54 - print / 56 - bprint
		Code.put(isBytePrint ? Code.bprint : Code.print);

		afterThenStatement(statementPrint);
	}

	@Override
	public void visit(StatementRead statementRead) {
		if (isSuppressed(statementRead)) return;

		Obj target = resolveDesignatorObj(statementRead.getDesignator());
		if (target == Tab.noObj) {
			return;
		}

		if (target.getKind() == Obj.Var) {
			// 53 - read / 55 - bread
			Code.put(target.getType() == Tab.charType ? Code.bread : Code.read);

			// 6..14 - odgovarajući store za Obj
			Code.store(target);

			afterThenStatement(statementRead);
			return;
		}

		if (target.getKind() == Obj.Elem) {
			// Designator je vec izgenerisao index; ukloni ga i regenerisi adr/index.
			// 39 - pop
			Code.put(Code.pop);

			Obj baseObj = findSymbol(statementRead.getDesignator().getName());
			DesignatorSuffix3 indexSuffix = getLastIndexSuffix(statementRead.getDesignator());
			if (baseObj == Tab.noObj || indexSuffix == null) {
				return;
			}

			// 1..14 - load base niza
			Code.load(baseObj);
			indexSuffix.getExpr().traverseBottomUp(this);

			Struct elemType = baseObj.getType().getElemType();

			// 53 - read / 55 - bread
			Code.put(elemType == Tab.charType ? Code.bread : Code.read);

			// 35 - astore / 37 - bastore
			Code.put(elemType == Tab.charType ? Code.bastore : Code.astore);

			afterThenStatement(statementRead);
		}
	}

	@Override
	public void visit(StatementReturn statementReturn) {
		if (isSuppressed(statementReturn)) return;

		if (statementReturn.getOptExpr() instanceof OptExpr1) {
			// expr je vec na steku
		}

		// 52 - exit
		Code.put(Code.exit);
		// 50 - return
		Code.put(Code.return_);

		afterThenStatement(statementReturn);
	}

	@Override
	public void visit(StatementDesignator statementDesignator) {
		if (isSuppressed(statementDesignator)) return;
		afterThenStatement(statementDesignator);
	}

	@Override
	public void visit(StatementStmtList statementStmtList) {
		if (isSuppressed(statementStmtList)) return;
		afterThenStatement(statementStmtList);
	}

	// ------------------------------------------------------------
	// DESIGNATOR STATEMENT
	// ------------------------------------------------------------

	@Override
	public void visit(DesignatorStatement designatorStatement) {
		/*
		 * DesignatorStatement pokriva:
		 * - dodelu
		 * - poziv funkcije kao statement
		 * - ++ / --
		 */
		if (isSuppressed(designatorStatement)) return;

		Obj target = resolveDesignatorObj(designatorStatement.getDesignator());
		if (target == Tab.noObj) {
			return;
		}

		// --------------------------------------------------------
		// DODELA
		// --------------------------------------------------------
		if (designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail1) {
			DesignatorStmtTail1 tail = (DesignatorStmtTail1) designatorStatement.getDesignatorStmtTail();

			if (target.getKind() == Obj.Var) {
				// 6..14 - odgovarajući store za Obj
				Code.store(target);
				return;
			}

			if (target.getKind() == Obj.Elem) {
				// Ukloni vec izgenerisan index i RHS, pa regenerisi redosled adr/index/val.
				// Store u niz mora da vidi: adr, index, value.

				// 39 - pop
				Code.put(Code.pop);
				// 39 - pop
				Code.put(Code.pop);

				Obj baseObj = findSymbol(designatorStatement.getDesignator().getName());
				DesignatorSuffix3 indexSuffix = getLastIndexSuffix(designatorStatement.getDesignator());

				if (baseObj == Tab.noObj || indexSuffix == null) {
					return;
				}

				// 1..14 - load base niza
				Code.load(baseObj);
				indexSuffix.getExpr().traverseBottomUp(this);
				tail.getExpr().traverseBottomUp(this);

				Struct elemType = baseObj.getType().getElemType();
				// 35 - astore / 37 - bastore
				Code.put(elemType == Tab.charType ? Code.bastore : Code.astore);
				return;
			}
		}

		// --------------------------------------------------------
		// POZIV FUNKCIJE KAO STATEMENT
		// --------------------------------------------------------
		if (designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail2) {
			emitFunctionCall(designatorStatement.getDesignator());

			if (target.getKind() == Obj.Meth && target.getType() != Tab.noType) {
				// rezultat funkcije se ne koristi -> skloni sa expr stack-a
				// 39 - pop
				Code.put(Code.pop);
			}
			return;
		}

		// --------------------------------------------------------
		// ++ / --
		// --------------------------------------------------------
		if (designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail3
				|| designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail4) {

			int op = designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail3
					? Code.add
					: Code.sub;

			if (target.getKind() == Obj.Var) {
				// 1..14 - load trenutne vrednosti
				Code.load(target);
				// 15..22 - const_* / const w
				Code.loadConst(1);
				// 23 - add / 24 - sub
				Code.put(op);
				// 6..14 - store nazad
				Code.store(target);
				return;
			}

			if (target.getKind() == Obj.Elem) {
				// Ukloni vec izgenerisan index i regenerisi adr/index.
				// ++/-- nad elementom niza je read-modify-write sekvenca.

				// 39 - pop
				Code.put(Code.pop);

				Obj baseObj = findSymbol(designatorStatement.getDesignator().getName());
				DesignatorSuffix3 indexSuffix = getLastIndexSuffix(designatorStatement.getDesignator());

				if (baseObj == Tab.noObj || indexSuffix == null) {
					return;
				}

				// 1..14 - load base niza
				Code.load(baseObj);
				indexSuffix.getExpr().traverseBottomUp(this);

				// 41 - dup2
				Code.put(Code.dup2);

				Struct elemType = baseObj.getType().getElemType();

				// 34 - aload / 36 - baload
				Code.put(elemType == Tab.charType ? Code.baload : Code.aload);
				// 15..22 - const_* / const w
				Code.loadConst(1);
				// 23 - add / 24 - sub
				Code.put(op);
				// 35 - astore / 37 - bastore
				Code.put(elemType == Tab.charType ? Code.bastore : Code.astore);
			}
		}
	}

	// ------------------------------------------------------------
	// FAKTORI
	// ------------------------------------------------------------

	@Override
	public void visit(FactorNumConst factorNumConst) {
		if (isInsideIfCondition(factorNumConst) && !forceEmit) return;
		if (isSuppressed(factorNumConst)) return;

		// 15..22 - const_* / const w
		Code.loadConst(factorNumConst.getN1());
	}

	@Override
	public void visit(FactorCharConst factorCharConst) {
		if (isInsideIfCondition(factorCharConst) && !forceEmit) return;
		if (isSuppressed(factorCharConst)) return;

		// 15..22 - const_* / const w
		Code.loadConst(factorCharConst.getC1());
	}

	@Override
	public void visit(FactorBoolConst factorBoolConst) {
		if (isInsideIfCondition(factorBoolConst) && !forceEmit) return;
		if (isSuppressed(factorBoolConst)) return;

		// 15..22 - const_* / const w
		Code.loadConst(factorBoolConst.getB1());
	}

	@Override
	public void visit(FactorNewArr factorNewArr) {
		if (isSuppressed(factorNewArr)) return;

		Struct elemType = resolveTypeSymbol(factorNewArr.getType());

		// 33 - newarray b
		Code.put(Code.newarray);
		Code.put(elemType == Tab.charType ? 0 : 1); // b = 0 za char[], b = 1 za ostale nizove
	}

	@Override
	public void visit(FactorDesignator factorDesignator) {
		// Ako je poziv metode -> emituj call, inace ucitaj vrednost designatora.
		if (isInsideIfCondition(factorDesignator) && !forceEmit) return;
		if (isSuppressed(factorDesignator)) return;

		if (factorDesignator.getOptCall() instanceof OptCall1) {
			emitFunctionCall(factorDesignator.getDesignator());
		} else {
			emitLoadDesignatorValue(factorDesignator.getDesignator());
		}
	}

	// ------------------------------------------------------------
	// IZRAZI
	// ------------------------------------------------------------

	@Override
	public void visit(Term2 term2) {
		// Operandi su vec na steku, ovde biramo samo aritmeticku instrukciju.
		if (isInsideIfCondition(term2) && !forceEmit) return;
		if (isSuppressed(term2)) return;

		if (term2.getMulop() instanceof MulopMUL) {
			// 25 - mul
			Code.put(Code.mul);
		} else if (term2.getMulop() instanceof MulopDIV) {
			// 26 - div
			Code.put(Code.div);
		} else {
			// 27 - rem
			Code.put(Code.rem);
		}
	}

	@Override
	public void visit(SimpleExpr2 simpleExpr2) {
		if (isInsideIfCondition(simpleExpr2) && !forceEmit) return;
		if (isSuppressed(simpleExpr2)) return;

		// 28 - neg
		Code.put(Code.neg);
	}

	@Override
	public void visit(SimpleExpr3 simpleExpr3) {
		if (isInsideIfCondition(simpleExpr3) && !forceEmit) return;
		if (isSuppressed(simpleExpr3)) return;

		if (simpleExpr3.getAddop() instanceof AddopADD) {
			// 23 - add
			Code.put(Code.add);
		} else {
			// 24 - sub
			Code.put(Code.sub);
		}
	}

	@Override
	public void visit(Expr2 expr2) {
		// Ternarni operator: uslov -> true grana -> skok preko false grane.
		if (isSuppressed(expr2)) return;

		// Deca su vec izracunala cond/true/false; zadrzi cond, odbaci preevaluirane grane.
		// 39 - pop
		Code.put(Code.pop);
		// 39 - pop
		Code.put(Code.pop);

		// 15..22 - const_* / const w
		Code.loadConst(0);

		// 43..48 - j<cond> s  (ovde preko Code.putFalseJump)
		Code.putFalseJump(Code.ne, 0);
		int falseBranchJump = Code.pc - 2;

		expr2.getExpr().traverseBottomUp(this);

		// 42 - jmp s
		Code.putJump(0);
		int endJump = Code.pc - 2;

		Code.fixup(falseBranchJump);
		expr2.getExpr1().traverseBottomUp(this);

		Code.fixup(endJump);
	}
}
