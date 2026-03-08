// generated with ast extension for cup
// version 0.8
// 8/2/2026 10:59:11


package rs.ac.bg.etf.pp1.ast;

public class ConstInitList1 extends ConstInitList {

    private ConstInit ConstInit;

    public ConstInitList1 (ConstInit ConstInit) {
        this.ConstInit=ConstInit;
        if(ConstInit!=null) ConstInit.setParent(this);
    }

    public ConstInit getConstInit() {
        return ConstInit;
    }

    public void setConstInit(ConstInit ConstInit) {
        this.ConstInit=ConstInit;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(ConstInit!=null) ConstInit.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(ConstInit!=null) ConstInit.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(ConstInit!=null) ConstInit.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("ConstInitList1(\n");

        if(ConstInit!=null)
            buffer.append(ConstInit.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [ConstInitList1]");
        return buffer.toString();
    }
}
