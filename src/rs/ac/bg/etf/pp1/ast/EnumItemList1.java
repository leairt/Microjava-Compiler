// generated with ast extension for cup
// version 0.8
// 5/2/2026 18:13:40


package rs.ac.bg.etf.pp1.ast;

public class EnumItemList1 extends EnumItemList {

    private EnumItem EnumItem;

    public EnumItemList1 (EnumItem EnumItem) {
        this.EnumItem=EnumItem;
        if(EnumItem!=null) EnumItem.setParent(this);
    }

    public EnumItem getEnumItem() {
        return EnumItem;
    }

    public void setEnumItem(EnumItem EnumItem) {
        this.EnumItem=EnumItem;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(EnumItem!=null) EnumItem.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(EnumItem!=null) EnumItem.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(EnumItem!=null) EnumItem.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("EnumItemList1(\n");

        if(EnumItem!=null)
            buffer.append(EnumItem.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [EnumItemList1]");
        return buffer.toString();
    }
}
