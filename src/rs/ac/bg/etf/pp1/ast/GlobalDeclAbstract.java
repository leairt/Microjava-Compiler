// generated with ast extension for cup
// version 0.8
// 30/0/2026 11:30:20


package rs.ac.bg.etf.pp1.ast;

public class GlobalDeclAbstract extends GlobalDecl {

    private AbstractDecl AbstractDecl;

    public GlobalDeclAbstract (AbstractDecl AbstractDecl) {
        this.AbstractDecl=AbstractDecl;
        if(AbstractDecl!=null) AbstractDecl.setParent(this);
    }

    public AbstractDecl getAbstractDecl() {
        return AbstractDecl;
    }

    public void setAbstractDecl(AbstractDecl AbstractDecl) {
        this.AbstractDecl=AbstractDecl;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(AbstractDecl!=null) AbstractDecl.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(AbstractDecl!=null) AbstractDecl.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(AbstractDecl!=null) AbstractDecl.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("GlobalDeclAbstract(\n");

        if(AbstractDecl!=null)
            buffer.append(AbstractDecl.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [GlobalDeclAbstract]");
        return buffer.toString();
    }
}
