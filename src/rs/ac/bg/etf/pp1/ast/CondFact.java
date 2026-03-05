// generated with ast extension for cup
// version 0.8
// 5/2/2026 18:13:40


package rs.ac.bg.etf.pp1.ast;

public class CondFact implements SyntaxNode {

    private SyntaxNode parent;
    private int line;
    private SimpleExpr SimpleExpr;
    private OptRel OptRel;

    public CondFact (SimpleExpr SimpleExpr, OptRel OptRel) {
        this.SimpleExpr=SimpleExpr;
        if(SimpleExpr!=null) SimpleExpr.setParent(this);
        this.OptRel=OptRel;
        if(OptRel!=null) OptRel.setParent(this);
    }

    public SimpleExpr getSimpleExpr() {
        return SimpleExpr;
    }

    public void setSimpleExpr(SimpleExpr SimpleExpr) {
        this.SimpleExpr=SimpleExpr;
    }

    public OptRel getOptRel() {
        return OptRel;
    }

    public void setOptRel(OptRel OptRel) {
        this.OptRel=OptRel;
    }

    public SyntaxNode getParent() {
        return parent;
    }

    public void setParent(SyntaxNode parent) {
        this.parent=parent;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line=line;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(SimpleExpr!=null) SimpleExpr.accept(visitor);
        if(OptRel!=null) OptRel.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(SimpleExpr!=null) SimpleExpr.traverseTopDown(visitor);
        if(OptRel!=null) OptRel.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(SimpleExpr!=null) SimpleExpr.traverseBottomUp(visitor);
        if(OptRel!=null) OptRel.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("CondFact(\n");

        if(SimpleExpr!=null)
            buffer.append(SimpleExpr.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        if(OptRel!=null)
            buffer.append(OptRel.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [CondFact]");
        return buffer.toString();
    }
}
