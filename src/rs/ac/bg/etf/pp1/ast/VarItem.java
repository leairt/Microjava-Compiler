// generated with ast extension for cup
// version 0.8
// 10/2/2026 12:28:29


package rs.ac.bg.etf.pp1.ast;

public class VarItem implements SyntaxNode {

    private SyntaxNode parent;
    private int line;
    private String name;
    private OptArray OptArray;

    public VarItem (String name, OptArray OptArray) {
        this.name=name;
        this.OptArray=OptArray;
        if(OptArray!=null) OptArray.setParent(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name=name;
    }

    public OptArray getOptArray() {
        return OptArray;
    }

    public void setOptArray(OptArray OptArray) {
        this.OptArray=OptArray;
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
        if(OptArray!=null) OptArray.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(OptArray!=null) OptArray.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(OptArray!=null) OptArray.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("VarItem(\n");

        buffer.append(" "+tab+name);
        buffer.append("\n");

        if(OptArray!=null)
            buffer.append(OptArray.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [VarItem]");
        return buffer.toString();
    }
}
