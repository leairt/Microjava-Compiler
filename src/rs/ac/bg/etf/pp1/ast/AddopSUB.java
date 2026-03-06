// generated with ast extension for cup
// version 0.8
// 6/2/2026 23:15:42


package rs.ac.bg.etf.pp1.ast;

public class AddopSUB extends Addop {

    public AddopSUB () {
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
        buffer.append("AddopSUB(\n");

        buffer.append(tab);
        buffer.append(") [AddopSUB]");
        return buffer.toString();
    }
}
