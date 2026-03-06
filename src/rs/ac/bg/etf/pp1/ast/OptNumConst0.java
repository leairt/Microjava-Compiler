// generated with ast extension for cup
// version 0.8
// 6/2/2026 15:32:10


package rs.ac.bg.etf.pp1.ast;

public class OptNumConst0 extends OptNumConst {

    public OptNumConst0 () {
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
        buffer.append("OptNumConst0(\n");

        buffer.append(tab);
        buffer.append(") [OptNumConst0]");
        return buffer.toString();
    }
}
