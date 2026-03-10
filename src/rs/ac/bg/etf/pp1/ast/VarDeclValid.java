// generated with ast extension for cup
// version 0.8
// 10/2/2026 12:28:29


package rs.ac.bg.etf.pp1.ast;

public class VarDeclValid extends VarDecl {

    private Type Type;
    private VarItemList VarItemList;

    public VarDeclValid (Type Type, VarItemList VarItemList) {
        this.Type=Type;
        if(Type!=null) Type.setParent(this);
        this.VarItemList=VarItemList;
        if(VarItemList!=null) VarItemList.setParent(this);
    }

    public Type getType() {
        return Type;
    }

    public void setType(Type Type) {
        this.Type=Type;
    }

    public VarItemList getVarItemList() {
        return VarItemList;
    }

    public void setVarItemList(VarItemList VarItemList) {
        this.VarItemList=VarItemList;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(Type!=null) Type.accept(visitor);
        if(VarItemList!=null) VarItemList.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(Type!=null) Type.traverseTopDown(visitor);
        if(VarItemList!=null) VarItemList.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(Type!=null) Type.traverseBottomUp(visitor);
        if(VarItemList!=null) VarItemList.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("VarDeclValid(\n");

        if(Type!=null)
            buffer.append(Type.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        if(VarItemList!=null)
            buffer.append(VarItemList.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [VarDeclValid]");
        return buffer.toString();
    }
}
