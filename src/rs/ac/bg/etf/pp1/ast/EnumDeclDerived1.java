// generated with ast extension for cup
// version 0.8
// 30/0/2026 11:30:20


package rs.ac.bg.etf.pp1.ast;

public class EnumDeclDerived1 extends EnumDecl {

    public EnumDeclDerived1 () {
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
        buffer.append("EnumDeclDerived1(\n");

        buffer.append(tab);
        buffer.append(") [EnumDeclDerived1]");
        return buffer.toString();
    }
}
