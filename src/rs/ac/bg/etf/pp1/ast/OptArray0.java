// generated with ast extension for cup
// version 0.8
// 8/2/2026 10:59:11


package rs.ac.bg.etf.pp1.ast;

public class OptArray0 extends OptArray {

    public OptArray0 () {
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
        buffer.append("OptArray0(\n");

        buffer.append(tab);
        buffer.append(") [OptArray0]");
        return buffer.toString();
    }
}
