// generated with ast extension for cup
// version 0.8
// 5/2/2026 18:13:40


package rs.ac.bg.etf.pp1.ast;

public class OptCondition0 extends OptCondition {

    public OptCondition0 () {
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
        buffer.append("OptCondition0(\n");

        buffer.append(tab);
        buffer.append(") [OptCondition0]");
        return buffer.toString();
    }
}
