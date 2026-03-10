// generated with ast extension for cup
// version 0.8
// 10/2/2026 12:28:29


package rs.ac.bg.etf.pp1.ast;

public class FactorBoolConst extends Factor {

    private Integer boolValue;

    public FactorBoolConst (Integer boolValue) {
        this.boolValue=boolValue;
    }

    public Integer getBoolValue() {
        return boolValue;
    }

    public void setBoolValue(Integer boolValue) {
        this.boolValue=boolValue;
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
        buffer.append("FactorBoolConst(\n");

        buffer.append(" "+tab+boolValue);
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [FactorBoolConst]");
        return buffer.toString();
    }
}
