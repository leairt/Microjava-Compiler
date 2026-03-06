// generated with ast extension for cup
// version 0.8
// 6/2/2026 15:32:10


package rs.ac.bg.etf.pp1.ast;

public class DesignatorSuffix1 extends DesignatorSuffix {

    private String field;

    public DesignatorSuffix1 (String field) {
        this.field=field;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field=field;
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
        buffer.append("DesignatorSuffix1(\n");

        buffer.append(" "+tab+field);
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [DesignatorSuffix1]");
        return buffer.toString();
    }
}
