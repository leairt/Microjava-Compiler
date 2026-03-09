// generated with ast extension for cup
// version 0.8
// 8/2/2026 15:28:25


package rs.ac.bg.etf.pp1.ast;

public class OptCall0 extends OptCall {

    public OptCall0 () {
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
        buffer.append("OptCall0(\n");

        buffer.append(tab);
        buffer.append(") [OptCall0]");
        return buffer.toString();
    }
}
