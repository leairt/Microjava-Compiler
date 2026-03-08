// generated with ast extension for cup
// version 0.8
// 8/2/2026 10:59:11


package rs.ac.bg.etf.pp1.ast;

public class Designator implements SyntaxNode {

    private SyntaxNode parent;
    private int line;
    private String name;
    private DesignatorSuffixList DesignatorSuffixList;

    public Designator (String name, DesignatorSuffixList DesignatorSuffixList) {
        this.name=name;
        this.DesignatorSuffixList=DesignatorSuffixList;
        if(DesignatorSuffixList!=null) DesignatorSuffixList.setParent(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name=name;
    }

    public DesignatorSuffixList getDesignatorSuffixList() {
        return DesignatorSuffixList;
    }

    public void setDesignatorSuffixList(DesignatorSuffixList DesignatorSuffixList) {
        this.DesignatorSuffixList=DesignatorSuffixList;
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
        if(DesignatorSuffixList!=null) DesignatorSuffixList.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(DesignatorSuffixList!=null) DesignatorSuffixList.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(DesignatorSuffixList!=null) DesignatorSuffixList.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("Designator(\n");

        buffer.append(" "+tab+name);
        buffer.append("\n");

        if(DesignatorSuffixList!=null)
            buffer.append(DesignatorSuffixList.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [Designator]");
        return buffer.toString();
    }
}
