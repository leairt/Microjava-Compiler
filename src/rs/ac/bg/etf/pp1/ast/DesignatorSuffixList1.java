// generated with ast extension for cup
// version 0.8
// 5/2/2026 18:6:45


package rs.ac.bg.etf.pp1.ast;

public class DesignatorSuffixList1 extends DesignatorSuffixList {

    private DesignatorSuffixList DesignatorSuffixList;
    private DesignatorSuffix DesignatorSuffix;

    public DesignatorSuffixList1 (DesignatorSuffixList DesignatorSuffixList, DesignatorSuffix DesignatorSuffix) {
        this.DesignatorSuffixList=DesignatorSuffixList;
        if(DesignatorSuffixList!=null) DesignatorSuffixList.setParent(this);
        this.DesignatorSuffix=DesignatorSuffix;
        if(DesignatorSuffix!=null) DesignatorSuffix.setParent(this);
    }

    public DesignatorSuffixList getDesignatorSuffixList() {
        return DesignatorSuffixList;
    }

    public void setDesignatorSuffixList(DesignatorSuffixList DesignatorSuffixList) {
        this.DesignatorSuffixList=DesignatorSuffixList;
    }

    public DesignatorSuffix getDesignatorSuffix() {
        return DesignatorSuffix;
    }

    public void setDesignatorSuffix(DesignatorSuffix DesignatorSuffix) {
        this.DesignatorSuffix=DesignatorSuffix;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(DesignatorSuffixList!=null) DesignatorSuffixList.accept(visitor);
        if(DesignatorSuffix!=null) DesignatorSuffix.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(DesignatorSuffixList!=null) DesignatorSuffixList.traverseTopDown(visitor);
        if(DesignatorSuffix!=null) DesignatorSuffix.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(DesignatorSuffixList!=null) DesignatorSuffixList.traverseBottomUp(visitor);
        if(DesignatorSuffix!=null) DesignatorSuffix.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("DesignatorSuffixList1(\n");

        if(DesignatorSuffixList!=null)
            buffer.append(DesignatorSuffixList.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        if(DesignatorSuffix!=null)
            buffer.append(DesignatorSuffix.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [DesignatorSuffixList1]");
        return buffer.toString();
    }
}
