// generated with ast extension for cup
// version 0.8
// 10/2/2026 12:28:29


package rs.ac.bg.etf.pp1.ast;

public class AbstractDeclDerived1 extends AbstractDecl {

    public AbstractDeclDerived1 () {
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("AbstractDeclDerived1(\n");

        buffer.append(tab);
        buffer.append(") [AbstractDeclDerived1]");
        return buffer.toString();
    }
}
