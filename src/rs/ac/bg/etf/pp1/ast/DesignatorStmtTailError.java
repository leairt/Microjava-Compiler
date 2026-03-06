// generated with ast extension for cup
// version 0.8
// 6/2/2026 13:0:50


package rs.ac.bg.etf.pp1.ast;

public class DesignatorStmtTailError extends DesignatorStmtTail {

    private Assignop Assignop;

    public DesignatorStmtTailError (Assignop Assignop) {
        this.Assignop=Assignop;
        if(Assignop!=null) Assignop.setParent(this);
    }

    public Assignop getAssignop() {
        return Assignop;
    }

    public void setAssignop(Assignop Assignop) {
        this.Assignop=Assignop;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(Assignop!=null) Assignop.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(Assignop!=null) Assignop.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(Assignop!=null) Assignop.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("DesignatorStmtTailError(\n");

        if(Assignop!=null)
            buffer.append(Assignop.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [DesignatorStmtTailError]");
        return buffer.toString();
    }
}
