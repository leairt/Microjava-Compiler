package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

import rs.ac.bg.etf.pp1.ast.ActPars;
import rs.ac.bg.etf.pp1.ast.AddopADD;
import rs.ac.bg.etf.pp1.ast.CondFact;
import rs.ac.bg.etf.pp1.ast.Designator;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffix;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffix1;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffix2;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffix3;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffixList;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffixList1;
import rs.ac.bg.etf.pp1.ast.DesignatorStatement;
import rs.ac.bg.etf.pp1.ast.DesignatorStmtTail1;
import rs.ac.bg.etf.pp1.ast.Expr;
import rs.ac.bg.etf.pp1.ast.Expr1;
import rs.ac.bg.etf.pp1.ast.Expr2;
import rs.ac.bg.etf.pp1.ast.Factor;
import rs.ac.bg.etf.pp1.ast.FactorBoolConst;
import rs.ac.bg.etf.pp1.ast.FactorCharConst;
import rs.ac.bg.etf.pp1.ast.FactorDesignator;
import rs.ac.bg.etf.pp1.ast.FactorExpr;
import rs.ac.bg.etf.pp1.ast.FactorNewArr;
import rs.ac.bg.etf.pp1.ast.FactorNumConst;
import rs.ac.bg.etf.pp1.ast.MethodDecl;
import rs.ac.bg.etf.pp1.ast.MethodName;
import rs.ac.bg.etf.pp1.ast.MulopDIV;
import rs.ac.bg.etf.pp1.ast.MulopMUL;
import rs.ac.bg.etf.pp1.ast.OptCall0;
import rs.ac.bg.etf.pp1.ast.OptCall1;
import rs.ac.bg.etf.pp1.ast.OptNumConst1;
import rs.ac.bg.etf.pp1.ast.OptRel0;
import rs.ac.bg.etf.pp1.ast.OptRel1;
import rs.ac.bg.etf.pp1.ast.ProgramName;
import rs.ac.bg.etf.pp1.ast.Relop;
import rs.ac.bg.etf.pp1.ast.RelopEQ;
import rs.ac.bg.etf.pp1.ast.RelopGE;
import rs.ac.bg.etf.pp1.ast.RelopGT;
import rs.ac.bg.etf.pp1.ast.RelopLE;
import rs.ac.bg.etf.pp1.ast.RelopLT;
import rs.ac.bg.etf.pp1.ast.RelopNEQ;
import rs.ac.bg.etf.pp1.ast.SimpleExpr;
import rs.ac.bg.etf.pp1.ast.SimpleExpr1;
import rs.ac.bg.etf.pp1.ast.SimpleExpr2;
import rs.ac.bg.etf.pp1.ast.SimpleExpr3;
import rs.ac.bg.etf.pp1.ast.StatementPrint;
import rs.ac.bg.etf.pp1.ast.Term;
import rs.ac.bg.etf.pp1.ast.Term1;
import rs.ac.bg.etf.pp1.ast.Term2;
import rs.ac.bg.etf.pp1.ast.Type;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPC;
	private String programName;

	private Obj currentProgram;
	private Obj currentMethod;

	public int getMainPC() {
		return this.mainPC;
	}

	@Override
	public void visit(ProgramName programName) {
		this.programName = programName.getName();
		this.currentProgram = Tab.find(programName.getName());
	}

	public String getProgramName() {
		return this.programName;
	}

	@Override
	public void visit(MethodName methodName) {
		if ("main".equals(methodName.getName())) {
			mainPC = Code.pc;
		}

		currentMethod = methodName.obj;
		currentMethod.setAdr(Code.pc);

		Code.put(Code.enter);
		Code.put(methodName.obj.getLevel());
		Code.put(methodName.obj.getLocalSymbols().size());
	}

	@Override
	public void visit(MethodDecl methodDecl) {
		Code.put(Code.exit);
		Code.put(Code.return_);
		currentMethod = null;
	}

	@Override
	public void visit(StatementPrint statementPrint) {
		boolean bytePrint = isCharExpression(statementPrint.getExpr());
		int width = bytePrint ? 1 : 0;

		if (statementPrint.getOptNumConst() instanceof OptNumConst1) {
			width = ((OptNumConst1) statementPrint.getOptNumConst()).getN1();
		}

		Code.loadConst(width);
		Code.put(bytePrint ? Code.bprint : Code.print);
	}
	
	@Override
	public void visit(DesignatorStatement designatorStatement) {
		if (!(designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail1)) {
			return;
		}
		
		Obj target = resolveDesignatorObj(designatorStatement.getDesignator());
		if (target == Tab.noObj) {
			return;
		}
		
		// Za sada podrzano direktno skladistenje u promenljivu (dovoljno za p006).
		if (target.getKind() == Obj.Var) {
			Code.store(target);
		}
	}

	@Override
	public void visit(FactorNumConst factorNumConst) {
		Code.loadConst(factorNumConst.getN1());
	}

	@Override
	public void visit(FactorCharConst factorCharConst) {
		Code.loadConst(factorCharConst.getC1());
	}

	@Override
	public void visit(FactorBoolConst factorBoolConst) {
		Code.loadConst(factorBoolConst.getB1());
	}

	@Override
	public void visit(FactorNewArr factorNewArr) {
		Struct elemType = resolveTypeSymbol(factorNewArr.getType());
		Code.put(Code.newarray);
		Code.put(elemType == Tab.charType ? 0 : 1);
	}

	@Override
	public void visit(FactorDesignator factorDesignator) {
		if (factorDesignator.getOptCall() instanceof OptCall1) {
			emitFunctionCall(factorDesignator.getDesignator());
		} else {
			emitLoadDesignatorValue(factorDesignator.getDesignator());
		}
	}

	@Override
	public void visit(Term2 term2) {
		if (term2.getMulop() instanceof MulopMUL) {
			Code.put(Code.mul);
		} else if (term2.getMulop() instanceof MulopDIV) {
			Code.put(Code.div);
		} else {
			Code.put(Code.rem);
		}
	}

	@Override
	public void visit(SimpleExpr2 simpleExpr2) {
		Code.put(Code.neg);
	}

	@Override
	public void visit(SimpleExpr3 simpleExpr3) {
		if (simpleExpr3.getAddop() instanceof AddopADD) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		}
	}

	@Override
	public void visit(CondFact condFact) {
		if (condFact.getOptRel() instanceof OptRel0) {
			return;
		}

		OptRel1 optRel1 = (OptRel1) condFact.getOptRel();
		int relop = mapRelop(optRel1.getRelop());

		int falseJump = Code.pc;
		Code.putFalseJump(relop, 0);

		Code.loadConst(1);
		int endJump = Code.pc;
		Code.putJump(0);

		Code.fixup(falseJump);
		Code.loadConst(0);
		Code.fixup(endJump);
	}

	@Override
	public void visit(Expr2 expr2) {
		// Stack pre: cond, trueExpr, falseExpr
		// dup_x2 (60), pop, dup_x1 (59), pop => falseExpr, trueExpr, cond
		Code.put(60);
		Code.put(Code.pop);
		Code.put(59);
		Code.put(Code.pop);

		Code.loadConst(0);
		int falseBranchJump = Code.pc;
		Code.putFalseJump(Code.ne, 0); // false kada cond == 0

		// true grana: ukloni falseExpr, ostavi trueExpr
		Code.put(59); // trueExpr, falseExpr, trueExpr
		Code.put(Code.pop);
		Code.put(Code.pop);
		int endJump = Code.pc;
		Code.putJump(0);

		// false grana: ukloni trueExpr, ostavi falseExpr
		Code.fixup(falseBranchJump);
		Code.put(Code.pop);

		Code.fixup(endJump);
	}

	private int mapRelop(Relop relop) {
		if (relop instanceof RelopEQ) return Code.eq;
		if (relop instanceof RelopNEQ) return Code.ne;
		if (relop instanceof RelopGT) return Code.gt;
		if (relop instanceof RelopGE) return Code.ge;
		if (relop instanceof RelopLT) return Code.lt;
		return Code.le;
	}

	private Struct resolveTypeSymbol(Type typeNode) {
		Obj typeObj = findSymbol(typeNode.getType());
		if (typeObj != Tab.noObj && typeObj.getKind() == Obj.Type) {
			return typeObj.getType();
		}
		return Tab.noType;
	}

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

	private void collectSuffixes(DesignatorSuffixList suffixList, List<DesignatorSuffix> out) {
		if (suffixList instanceof DesignatorSuffixList1) {
			DesignatorSuffixList1 list1 = (DesignatorSuffixList1) suffixList;
			collectSuffixes(list1.getDesignatorSuffixList(), out);
			out.add(list1.getDesignatorSuffix());
		}
	}

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

	private void emitLoadDesignatorValue(Designator designator) {
		Obj baseObj = findSymbol(designator.getName());
		if (baseObj == Tab.noObj) {
			return;
		}

		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(designator.getDesignatorSuffixList(), suffixes);

		if (suffixes.isEmpty()) {
			Code.load(baseObj);
			return;
		}

		DesignatorSuffix last = suffixes.get(suffixes.size() - 1);

		if (last instanceof DesignatorSuffix2) {
			Code.load(baseObj);
			Code.put(Code.arraylength);
			return;
		}

		if (last instanceof DesignatorSuffix1) {
			Obj resolved = resolveDesignatorObj(designator);
			if (resolved != Tab.noObj) {
				Code.load(resolved);
			}
			return;
		}

		// suffix3: indeks je vec na steku, ucitamo niz i zamenimo redosled na adr,index
		Code.load(baseObj);
		Code.put(59); // dup_x1
		Code.put(Code.pop);

		Struct elemType = baseObj.getType().getElemType();
		Code.put(elemType == Tab.charType ? Code.baload : Code.aload);
	}

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
			Code.put(Code.arraylength);
			return;
		}

		int offset = funcObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}

	private boolean isCharExpression(Expr expr) {
		if (expr instanceof Expr1) {
			return isCharSimpleExpr(((Expr1) expr).getSimpleExpr());
		}
		return false;
	}

	private boolean isCharSimpleExpr(SimpleExpr simpleExpr) {
		if (simpleExpr instanceof SimpleExpr1) {
			return isCharTerm(((SimpleExpr1) simpleExpr).getTerm());
		}
		return false;
	}

	private boolean isCharTerm(Term term) {
		if (term instanceof Term1) {
			return isCharFactor(((Term1) term).getFactor());
		}
		return false;
	}

	private boolean isCharFactor(Factor factor) {
		if (factor instanceof FactorCharConst) {
			return true;
		}

		if (factor instanceof FactorExpr) {
			return isCharExpression(((FactorExpr) factor).getExpr());
		}

		if (factor instanceof FactorDesignator) {
			FactorDesignator factorDesignator = (FactorDesignator) factor;
			if (factorDesignator.getOptCall() instanceof OptCall0) {
				return isCharDesignator(factorDesignator.getDesignator());
			}
		}

		return false;
	}

	private boolean isCharDesignator(Designator designator) {
		Struct type = resolveDesignatorType(designator);
		return type == Tab.charType;
	}

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
}
