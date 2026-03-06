// generated with ast extension for cup
// version 0.8
// 6/2/2026 13:0:50


package rs.ac.bg.etf.pp1.ast;

public class EnumItemList2 extends EnumItemList {

    private EnumItemList EnumItemList;
    private EnumItem EnumItem;

    public EnumItemList2 (EnumItemList EnumItemList, EnumItem EnumItem) {
        this.EnumItemList=EnumItemList;
        if(EnumItemList!=null) EnumItemList.setParent(this);
        this.EnumItem=EnumItem;
        if(EnumItem!=null) EnumItem.setParent(this);
    }

    public EnumItemList getEnumItemList() {
        return EnumItemList;
    }

    public void setEnumItemList(EnumItemList EnumItemList) {
        this.EnumItemList=EnumItemList;
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
        if(EnumItemList!=null) EnumItemList.accept(visitor);
        if(EnumItem!=null) EnumItem.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(EnumItemList!=null) EnumItemList.traverseTopDown(visitor);
        if(EnumItem!=null) EnumItem.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(EnumItemList!=null) EnumItemList.traverseBottomUp(visitor);
        if(EnumItem!=null) EnumItem.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("EnumItemList2(\n");

        if(EnumItemList!=null)
            buffer.append(EnumItemList.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        if(EnumItem!=null)
            buffer.append(EnumItem.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [EnumItemList2]");
        return buffer.toString();
    }
}
