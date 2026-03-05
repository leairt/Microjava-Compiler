// generated with ast extension for cup
// version 0.8
// 5/2/2026 18:6:45


package rs.ac.bg.etf.pp1.ast;

public class VarDeclList0 extends VarDeclList {

    public VarDeclList0 () {
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
        buffer.append("VarDeclList0(\n");

        buffer.append(tab);
        buffer.append(") [VarDeclList0]");
        return buffer.toString();
    }
}
