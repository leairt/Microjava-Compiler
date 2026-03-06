// generated with ast extension for cup
// version 0.8
// 6/2/2026 23:15:42


package rs.ac.bg.etf.pp1.ast;

public class OptRel1 extends OptRel {

    private Relop Relop;
    private SimpleExpr SimpleExpr;

    public OptRel1 (Relop Relop, SimpleExpr SimpleExpr) {
        this.Relop=Relop;
        if(Relop!=null) Relop.setParent(this);
        this.SimpleExpr=SimpleExpr;
        if(SimpleExpr!=null) SimpleExpr.setParent(this);
    }

    public Relop getRelop() {
        return Relop;
    }

    public void setRelop(Relop Relop) {
        this.Relop=Relop;
    }

    public SimpleExpr getSimpleExpr() {
        return SimpleExpr;
    }

    public void setSimpleExpr(SimpleExpr SimpleExpr) {
        this.SimpleExpr=SimpleExpr;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(Relop!=null) Relop.accept(visitor);
        if(SimpleExpr!=null) SimpleExpr.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(Relop!=null) Relop.traverseTopDown(visitor);
        if(SimpleExpr!=null) SimpleExpr.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(Relop!=null) Relop.traverseBottomUp(visitor);
        if(SimpleExpr!=null) SimpleExpr.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("OptRel1(\n");

        if(Relop!=null)
            buffer.append(Relop.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        if(SimpleExpr!=null)
            buffer.append(SimpleExpr.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [OptRel1]");
        return buffer.toString();
    }
}
