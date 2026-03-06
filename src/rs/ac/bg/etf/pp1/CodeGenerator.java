package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rs.ac.bg.etf.pp1.ast.ActPars;
import rs.ac.bg.etf.pp1.ast.AddopADD;
import rs.ac.bg.etf.pp1.ast.Case;
import rs.ac.bg.etf.pp1.ast.CaseList;
import rs.ac.bg.etf.pp1.ast.CaseList0;
import rs.ac.bg.etf.pp1.ast.CaseList1;
import rs.ac.bg.etf.pp1.ast.CondFact;
import rs.ac.bg.etf.pp1.ast.Condition;
import rs.ac.bg.etf.pp1.ast.Designator;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffix;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffix1;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffix2;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffix3;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffixList;
import rs.ac.bg.etf.pp1.ast.DesignatorSuffixList1;
import rs.ac.bg.etf.pp1.ast.DesignatorStatement;
import rs.ac.bg.etf.pp1.ast.DesignatorStmtTail1;
import rs.ac.bg.etf.pp1.ast.DesignatorStmtTail2;
import rs.ac.bg.etf.pp1.ast.DesignatorStmtTail3;
import rs.ac.bg.etf.pp1.ast.DesignatorStmtTail4;
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
import rs.ac.bg.etf.pp1.ast.OptCondition0;
import rs.ac.bg.etf.pp1.ast.OptCondition1;
import rs.ac.bg.etf.pp1.ast.OptCall0;
import rs.ac.bg.etf.pp1.ast.OptCall1;
import rs.ac.bg.etf.pp1.ast.OptDesignatorStatement;
import rs.ac.bg.etf.pp1.ast.OptDesignatorStatement0;
import rs.ac.bg.etf.pp1.ast.OptDesignatorStatement1;
import rs.ac.bg.etf.pp1.ast.OptElse0;
import rs.ac.bg.etf.pp1.ast.OptElse1;
import rs.ac.bg.etf.pp1.ast.OptExpr1;
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
import rs.ac.bg.etf.pp1.ast.Statement;
import rs.ac.bg.etf.pp1.ast.StatementBreak;
import rs.ac.bg.etf.pp1.ast.StatementContinue;
import rs.ac.bg.etf.pp1.ast.StatementDesignator;
import rs.ac.bg.etf.pp1.ast.StatementFor;
import rs.ac.bg.etf.pp1.ast.StatementIf;
import rs.ac.bg.etf.pp1.ast.StatementPrint;
import rs.ac.bg.etf.pp1.ast.StatementRead;
import rs.ac.bg.etf.pp1.ast.StatementReturn;
import rs.ac.bg.etf.pp1.ast.StatementStmtList;
import rs.ac.bg.etf.pp1.ast.StatementSwitch;
import rs.ac.bg.etf.pp1.ast.StatementList;
import rs.ac.bg.etf.pp1.ast.StatementList0;
import rs.ac.bg.etf.pp1.ast.StatementList1;
import rs.ac.bg.etf.pp1.ast.SyntaxNode;
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
	
	private final Map<StatementIf, List<Integer>> ifFalseJumps = new HashMap<>();
	private final Map<StatementIf, Integer> ifEndJumps = new HashMap<>();
	
	private static class LoopContext {
		StatementFor node;
		int condStart = -1;
		int bodyJump = -1;
		int falseJump = -1;
		int updateStart = -1;
		List<Integer> breakJumps = new ArrayList<>();
		List<Integer> continueJumps = new ArrayList<>();
	}
	private final Map<StatementFor, LoopContext> forContexts = new HashMap<>();
	private final List<LoopContext> loopStack = new ArrayList<>();
	
	private static class SwitchContext {
		StatementSwitch node;
		List<Integer> breakJumps = new ArrayList<>();
	}
	private final List<SwitchContext> switchStack = new ArrayList<>();
	
	private final Set<SyntaxNode> suppressedRoots = new HashSet<>();
	private boolean forceEmit = false;
	
	private static class BoolLists {
		List<Integer> trueJumps = new ArrayList<>();
		List<Integer> falseJumps = new ArrayList<>();
	}

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
		if (isSuppressed(statementPrint)) return;
		boolean bytePrint = isCharExpression(statementPrint.getExpr());
		int width = bytePrint ? 1 : 0;

		if (statementPrint.getOptNumConst() instanceof OptNumConst1) {
			width = ((OptNumConst1) statementPrint.getOptNumConst()).getN1();
		}

		Code.loadConst(width);
		Code.put(bytePrint ? Code.bprint : Code.print);
		afterThenStatement(statementPrint);
	}
	
	@Override
	public void visit(DesignatorStatement designatorStatement) {
		if (isSuppressed(designatorStatement)) return;
		Obj target = resolveDesignatorObj(designatorStatement.getDesignator());
		if (target == Tab.noObj) {
			return;
		}

		if (designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail1) {
			DesignatorStmtTail1 tail = (DesignatorStmtTail1) designatorStatement.getDesignatorStmtTail();

			if (target.getKind() == Obj.Var) {
				Code.store(target);
				return;
			}

			if (target.getKind() == Obj.Elem) {
				// Ukloni vec izgenerisan index i RHS, pa regenerisi redosled adr/index/val.
				Code.put(Code.pop);
				Code.put(Code.pop);

				Obj baseObj = findSymbol(designatorStatement.getDesignator().getName());
				DesignatorSuffix3 indexSuffix = getLastIndexSuffix(designatorStatement.getDesignator());
				if (baseObj == Tab.noObj || indexSuffix == null) {
					return;
				}

				Code.load(baseObj);
				indexSuffix.getExpr().traverseBottomUp(this);
				tail.getExpr().traverseBottomUp(this);

				Struct elemType = baseObj.getType().getElemType();
				Code.put(elemType == Tab.charType ? Code.bastore : Code.astore);
				return;
			}
		}

		if (designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail2) {
			emitFunctionCall(designatorStatement.getDesignator());
			if (target.getKind() == Obj.Meth && target.getType() != Tab.noType) {
				Code.put(Code.pop);
			}
			return;
		}

		if (designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail3
				|| designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail4) {
			int op = designatorStatement.getDesignatorStmtTail() instanceof DesignatorStmtTail3 ? Code.add : Code.sub;

			if (target.getKind() == Obj.Var) {
				Code.load(target);
				Code.loadConst(1);
				Code.put(op);
				Code.store(target);
				return;
			}

			if (target.getKind() == Obj.Elem) {
				// Ukloni vec izgenerisan index i regenerisi adr/index.
				Code.put(Code.pop);

				Obj baseObj = findSymbol(designatorStatement.getDesignator().getName());
				DesignatorSuffix3 indexSuffix = getLastIndexSuffix(designatorStatement.getDesignator());
				if (baseObj == Tab.noObj || indexSuffix == null) {
					return;
				}

				Code.load(baseObj);
				indexSuffix.getExpr().traverseBottomUp(this);
				Code.put(Code.dup2);

				Struct elemType = baseObj.getType().getElemType();
				Code.put(elemType == Tab.charType ? Code.baload : Code.aload);
				Code.loadConst(1);
				Code.put(op);
				Code.put(elemType == Tab.charType ? Code.bastore : Code.astore);
			}
		}
	}

	@Override
	public void visit(FactorNumConst factorNumConst) {
		if (isInsideIfCondition(factorNumConst) && !forceEmit) return;
		if (isSuppressed(factorNumConst)) return;
		Code.loadConst(factorNumConst.getN1());
	}

	@Override
	public void visit(FactorCharConst factorCharConst) {
		if (isInsideIfCondition(factorCharConst) && !forceEmit) return;
		if (isSuppressed(factorCharConst)) return;
		Code.loadConst(factorCharConst.getC1());
	}

	@Override
	public void visit(FactorBoolConst factorBoolConst) {
		if (isInsideIfCondition(factorBoolConst) && !forceEmit) return;
		if (isSuppressed(factorBoolConst)) return;
		Code.loadConst(factorBoolConst.getB1());
	}

	@Override
	public void visit(FactorNewArr factorNewArr) {
		if (isSuppressed(factorNewArr)) return;
		Struct elemType = resolveTypeSymbol(factorNewArr.getType());
		Code.put(Code.newarray);
		Code.put(elemType == Tab.charType ? 0 : 1);
	}

	@Override
	public void visit(FactorDesignator factorDesignator) {
		if (isInsideIfCondition(factorDesignator) && !forceEmit) return;
		if (isSuppressed(factorDesignator)) return;
		if (factorDesignator.getOptCall() instanceof OptCall1) {
			emitFunctionCall(factorDesignator.getDesignator());
		} else {
			emitLoadDesignatorValue(factorDesignator.getDesignator());
		}
	}

	@Override
	public void visit(Term2 term2) {
		if (isInsideIfCondition(term2) && !forceEmit) return;
		if (isSuppressed(term2)) return;
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
		if (isInsideIfCondition(simpleExpr2) && !forceEmit) return;
		if (isSuppressed(simpleExpr2)) return;
		Code.put(Code.neg);
	}

	@Override
	public void visit(SimpleExpr3 simpleExpr3) {
		if (isInsideIfCondition(simpleExpr3) && !forceEmit) return;
		if (isSuppressed(simpleExpr3)) return;
		if (simpleExpr3.getAddop() instanceof AddopADD) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		}
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

		Code.putFalseJump(relop, 0);
		int falseJump = Code.pc - 2;

		Code.loadConst(1);
		Code.putJump(0);
		int endJump = Code.pc - 2;

		Code.fixup(falseJump);
		Code.loadConst(0);
		Code.fixup(endJump);
	}

	@Override
	public void visit(Expr2 expr2) {
		if (isSuppressed(expr2)) return;
		// Deca su vec izracunala cond/true/false; zadrzi cond, odbaci preevaluirane grane.
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.loadConst(0);
		Code.putFalseJump(Code.ne, 0);
		int falseBranchJump = Code.pc - 2;

		expr2.getExpr().traverseBottomUp(this);
		Code.putJump(0);
		int endJump = Code.pc - 2;

		Code.fixup(falseBranchJump);
		expr2.getExpr1().traverseBottomUp(this);

		Code.fixup(endJump);
	}

	private void handleIfCondition(SyntaxNode conditionNode) {
		if (isSuppressed(conditionNode)) return;
		if (conditionNode.getParent() instanceof StatementIf) {
			BoolLists lists = emitConditionLists(conditionNode);
			fixJumps(lists.trueJumps, Code.pc);
			ifFalseJumps.put((StatementIf) conditionNode.getParent(), lists.falseJumps);
		}
	}
	
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
		Code.loadConst(0);
		Code.putFalseJump(Code.ne, 0);
		ctx.falseJump = Code.pc - 2;
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
		Code.putJump(0);
		ctx.bodyJump = Code.pc - 2;
		ctx.updateStart = Code.pc;
	}

	@Override
	public void visit(StatementFor statementFor) {
		if (isSuppressed(statementFor)) return;
		LoopContext ctx = forContexts.remove(statementFor);
		if (ctx == null) return;
		loopStack.remove(ctx);
		if (ctx.updateStart < 0) ctx.updateStart = ctx.condStart;
		Code.putJump(ctx.updateStart);
		int loopEnd = Code.pc;
		if (ctx.falseJump >= 0) Code.fixup(ctx.falseJump);
		for (Integer adr : ctx.breakJumps) Code.fixup(adr);
		for (Integer adr : ctx.continueJumps) Code.put2(adr, ctx.updateStart - adr + 1);
		afterThenStatement(statementFor);
	}

	@Override
	public void visit(StatementBreak statementBreak) {
		if (isSuppressed(statementBreak)) return;
		if (!switchStack.isEmpty()) {
			Code.putJump(0);
			switchStack.get(switchStack.size() - 1).breakJumps.add(Code.pc - 2);
			afterThenStatement(statementBreak);
			return;
		}
		if (!loopStack.isEmpty()) {
			Code.putJump(0);
			loopStack.get(loopStack.size() - 1).breakJumps.add(Code.pc - 2);
		}
		afterThenStatement(statementBreak);
	}

	@Override
	public void visit(StatementContinue statementContinue) {
		if (isSuppressed(statementContinue)) return;
		if (!loopStack.isEmpty()) {
			LoopContext ctx = loopStack.get(loopStack.size() - 1);
			if (ctx.updateStart >= 0) {
				Code.putJump(ctx.updateStart);
			} else {
				Code.putJump(0);
				ctx.continueJumps.add(Code.pc - 2);
			}
		}
		afterThenStatement(statementContinue);
	}

	@Override
	public void visit(StatementReturn statementReturn) {
		if (isSuppressed(statementReturn)) return;
		if (statementReturn.getOptExpr() instanceof OptExpr1) {
			// expr je vec na steku
		}
		Code.put(Code.exit);
		Code.put(Code.return_);
		afterThenStatement(statementReturn);
	}

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
		if (isSuppressed(statementSwitch)) return;
		// Ukloni vec izracunatu vrednost switch izraza i ručno generiši case grananje.
		Code.put(Code.pop);
		List<Case> cases = new ArrayList<>();
		collectCases(statementSwitch.getCaseList(), cases);
		List<Integer> caseBodyJumps = new ArrayList<>();
		SwitchContext sw = new SwitchContext();
		sw.node = statementSwitch;
		switchStack.add(sw);
		for (Case c : cases) {
			statementSwitch.getExpr().traverseBottomUp(this);
			Code.loadConst(c.getN1());
			Code.putFalseJump(Code.eq, 0);
			int notEq = Code.pc - 2;
			Code.putJump(0);
			caseBodyJumps.add(Code.pc - 2);
			Code.fixup(notEq);
		}
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

	private boolean isInsideIfCondition(SyntaxNode node) {
		SyntaxNode cur = node;
		SyntaxNode condRoot = null;
		while (cur != null) {
			if (cur instanceof rs.ac.bg.etf.pp1.ast.Condition1 || cur instanceof rs.ac.bg.etf.pp1.ast.Condition2) {
				condRoot = cur;
			}
			if (cur instanceof StatementIf) {
				StatementIf si = (StatementIf) cur;
				return condRoot != null && si.getCondition() == condRoot;
			}
			cur = cur.getParent();
		}
		return false;
	}

	private void fixJumps(List<Integer> jumps, int targetPc) {
		for (Integer adr : jumps) {
			Code.put2(adr, targetPc - adr + 1);
		}
	}

	private BoolLists emitCondFactLists(CondFact cf) {
		BoolLists bl = new BoolLists();
		boolean old = forceEmit;
		forceEmit = true;
		try {
			if (cf.getOptRel() instanceof OptRel1) {
				OptRel1 rel = (OptRel1) cf.getOptRel();
				cf.getSimpleExpr().traverseBottomUp(this);
				rel.getSimpleExpr().traverseBottomUp(this);
				int op = mapRelop(rel.getRelop());
				Code.putFalseJump(op, 0);
				bl.falseJumps.add(Code.pc - 2);
				Code.putJump(0);
				bl.trueJumps.add(Code.pc - 2);
			} else {
				cf.getSimpleExpr().traverseBottomUp(this);
				Code.loadConst(0);
				Code.putFalseJump(Code.ne, 0);
				bl.falseJumps.add(Code.pc - 2);
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

	@Override
	public void visit(StatementDesignator statementDesignator) {
		if (isSuppressed(statementDesignator)) return;
		afterThenStatement(statementDesignator);
	}

	@Override
	public void visit(StatementRead statementRead) {
		if (isSuppressed(statementRead)) return;
		Obj target = resolveDesignatorObj(statementRead.getDesignator());
		if (target == Tab.noObj) {
			return;
		}

		if (target.getKind() == Obj.Var) {
			Code.put(target.getType() == Tab.charType ? Code.bread : Code.read);
			Code.store(target);
			afterThenStatement(statementRead);
			return;
		}

		if (target.getKind() == Obj.Elem) {
			// Designator je vec izgenerisao index; ukloni ga i regenerisi adr/index.
			Code.put(Code.pop);

			Obj baseObj = findSymbol(statementRead.getDesignator().getName());
			DesignatorSuffix3 indexSuffix = getLastIndexSuffix(statementRead.getDesignator());
			if (baseObj == Tab.noObj || indexSuffix == null) {
				return;
			}

			Code.load(baseObj);
			indexSuffix.getExpr().traverseBottomUp(this);
			Struct elemType = baseObj.getType().getElemType();
			Code.put(elemType == Tab.charType ? Code.bread : Code.read);
			Code.put(elemType == Tab.charType ? Code.bastore : Code.astore);
			afterThenStatement(statementRead);
		}
	}

	@Override
	public void visit(StatementStmtList statementStmtList) {
		if (isSuppressed(statementStmtList)) return;
		afterThenStatement(statementStmtList);
	}

	private int mapRelop(Relop relop) {
		if (relop instanceof RelopEQ) return Code.eq;
		if (relop instanceof RelopNEQ) return Code.ne;
		if (relop instanceof RelopGT) return Code.gt;
		if (relop instanceof RelopGE) return Code.ge;
		if (relop instanceof RelopLT) return Code.lt;
		return Code.le;
	}

	private boolean isSuppressed(SyntaxNode node) {
		if (forceEmit || node == null) return false;
		SyntaxNode cur = node;
		while (cur != null) {
			if (suppressedRoots.contains(cur)) return true;
			cur = cur.getParent();
		}
		boolean inCase = false;
		cur = node;
		while (cur != null) {
			if (cur instanceof Case) inCase = true;
			if (cur instanceof StatementSwitch) {
				if (inCase) return true;
				break;
			}
			cur = cur.getParent();
		}
		return false;
	}

	private void afterThenStatement(Statement statementNode) {
		SyntaxNode p = statementNode.getParent();
		if (!(p instanceof StatementIf)) return;
		StatementIf ifNode = (StatementIf) p;
		if (ifNode.getStatement() != statementNode) return;
		if (!(ifNode.getOptElse() instanceof OptElse1)) return;
		Code.putJump(0);
		ifEndJumps.put(ifNode, Code.pc - 2);
		List<Integer> falseJumps = ifFalseJumps.remove(ifNode);
		if (falseJumps != null) fixJumps(falseJumps, Code.pc);
	}

	private void collectCases(CaseList list, List<Case> out) {
		if (list instanceof CaseList1) {
			CaseList1 l = (CaseList1) list;
			collectCases(l.getCaseList(), out);
			out.add(l.getCase());
		}
	}

	private void emitStatementList(StatementList list) {
		boolean old = forceEmit;
		forceEmit = true;
		try {
			if (list != null) list.traverseBottomUp(this);
		} finally {
			forceEmit = old;
		}
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

		// suffix3: ukloni vec izracunat index i regenerisi adr/index.
		Code.put(Code.pop);
		DesignatorSuffix3 indexSuffix = getLastIndexSuffix(designator);
		if (indexSuffix == null) {
			return;
		}
		Code.load(baseObj);
		indexSuffix.getExpr().traverseBottomUp(this);

		Struct elemType = baseObj.getType().getElemType();
		Code.put(elemType == Tab.charType ? Code.baload : Code.aload);
	}

	private DesignatorSuffix3 getLastIndexSuffix(Designator designator) {
		List<DesignatorSuffix> suffixes = new ArrayList<>();
		collectSuffixes(designator.getDesignatorSuffixList(), suffixes);
		if (suffixes.isEmpty()) {
			return null;
		}
		DesignatorSuffix last = suffixes.get(suffixes.size() - 1);
		return last instanceof DesignatorSuffix3 ? (DesignatorSuffix3) last : null;
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
		if (expr instanceof Expr2) {
			Expr2 ternary = (Expr2) expr;
			return isCharExpression(ternary.getExpr()) && isCharExpression(ternary.getExpr1());
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
