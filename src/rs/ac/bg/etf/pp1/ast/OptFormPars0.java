// generated with ast extension for cup
// version 0.8
// 5/2/2026 18:6:45


package rs.ac.bg.etf.pp1.ast;

public class OptFormPars0 extends OptFormPars {

    public OptFormPars0 () {
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
        buffer.append("OptFormPars0(\n");

        buffer.append(tab);
        buffer.append(") [OptFormPars0]");
        return buffer.toString();
    }
}
