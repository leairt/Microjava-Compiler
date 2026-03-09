// generated with ast extension for cup
// version 0.8
// 8/2/2026 15:28:25


package rs.ac.bg.etf.pp1.ast;

public class ConstInitList2 extends ConstInitList {

    private ConstInitList ConstInitList;
    private ConstInit ConstInit;

    public ConstInitList2 (ConstInitList ConstInitList, ConstInit ConstInit) {
        this.ConstInitList=ConstInitList;
        if(ConstInitList!=null) ConstInitList.setParent(this);
        this.ConstInit=ConstInit;
        if(ConstInit!=null) ConstInit.setParent(this);
    }

    public ConstInitList getConstInitList() {
        return ConstInitList;
    }

    public void setConstInitList(ConstInitList ConstInitList) {
        this.ConstInitList=ConstInitList;
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
        if(ConstInitList!=null) ConstInitList.accept(visitor);
        if(ConstInit!=null) ConstInit.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(ConstInitList!=null) ConstInitList.traverseTopDown(visitor);
        if(ConstInit!=null) ConstInit.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(ConstInitList!=null) ConstInitList.traverseBottomUp(visitor);
        if(ConstInit!=null) ConstInit.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("ConstInitList2(\n");

        if(ConstInitList!=null)
            buffer.append(ConstInitList.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        if(ConstInit!=null)
            buffer.append(ConstInit.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [ConstInitList2]");
        return buffer.toString();
    }
}
