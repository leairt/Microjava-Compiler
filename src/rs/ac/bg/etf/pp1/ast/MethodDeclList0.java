// generated with ast extension for cup
// version 0.8
// 30/0/2026 11:30:20


package rs.ac.bg.etf.pp1.ast;

public class MethodDeclList0 extends MethodDeclList {

    public MethodDeclList0 () {
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
        buffer.append("MethodDeclList0(\n");

        buffer.append(tab);
        buffer.append(") [MethodDeclList0]");
        return buffer.toString();
    }
}
