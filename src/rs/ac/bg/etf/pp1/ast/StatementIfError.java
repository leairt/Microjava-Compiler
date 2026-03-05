// generated with ast extension for cup
// version 0.8
// 5/2/2026 18:13:40


package rs.ac.bg.etf.pp1.ast;

public class StatementIfError extends Statement {

    private Statement Statement;
    private OptElse OptElse;

    public StatementIfError (Statement Statement, OptElse OptElse) {
        this.Statement=Statement;
        if(Statement!=null) Statement.setParent(this);
        this.OptElse=OptElse;
        if(OptElse!=null) OptElse.setParent(this);
    }

    public Statement getStatement() {
        return Statement;
    }

    public void setStatement(Statement Statement) {
        this.Statement=Statement;
    }

    public OptElse getOptElse() {
        return OptElse;
    }

    public void setOptElse(OptElse OptElse) {
        this.OptElse=OptElse;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void childrenAccept(Visitor visitor) {
        if(Statement!=null) Statement.accept(visitor);
        if(OptElse!=null) OptElse.accept(visitor);
    }

    public void traverseTopDown(Visitor visitor) {
        accept(visitor);
        if(Statement!=null) Statement.traverseTopDown(visitor);
        if(OptElse!=null) OptElse.traverseTopDown(visitor);
    }

    public void traverseBottomUp(Visitor visitor) {
        if(Statement!=null) Statement.traverseBottomUp(visitor);
        if(OptElse!=null) OptElse.traverseBottomUp(visitor);
        accept(visitor);
    }

    public String toString(String tab) {
        StringBuffer buffer=new StringBuffer();
        buffer.append(tab);
        buffer.append("StatementIfError(\n");

        if(Statement!=null)
            buffer.append(Statement.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        if(OptElse!=null)
            buffer.append(OptElse.toString("  "+tab));
        else
            buffer.append(tab+"  null");
        buffer.append("\n");

        buffer.append(tab);
        buffer.append(") [StatementIfError]");
        return buffer.toString();
    }
}
