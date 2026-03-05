
package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}
	
	// za grupisanje uzastopnih neprepoznatih karaktera
	private StringBuilder errorBuffer = new StringBuilder();
	private int errorLine = -1;
	private int errorColumn = -1;

%}

%cup
%line
%column

%xstate COMMENT
%xstate ERROR_STATE

%eofval{
	if (errorBuffer.length() > 0) {
		String errorText = errorBuffer.toString();
		int line = errorLine;
		int col = errorColumn;
		errorBuffer.setLength(0);
		System.err.println("Leksicka greska (\"" + errorText + "\") u liniji " + line + ", kolona " + col);
	}
	return new_symbol(sym.EOF);
%eofval}

%%

" " { }
\t  { }
\b  { }
\r  { }
\n  { }
\f  { }


"//" {yybegin(COMMENT);}
<COMMENT> . {yybegin(COMMENT);}
<COMMENT> "\r" { yybegin(YYINITIAL); }
<COMMENT> "\n" { yybegin(YYINITIAL); }
 
 
"program"   { return new_symbol(sym.PROGRAM, yytext()); }
"break"     { return new_symbol(sym.BREAK, yytext()); }
"enum"      { return new_symbol(sym.ENUM, yytext()); }
"class"     { return new_symbol(sym.CLASS, yytext()); }
"abstract"  { return new_symbol(sym.ABSTRACT, yytext()); }
"else"      { return new_symbol(sym.ELSE, yytext()); }
"const"     { return new_symbol(sym.CONST, yytext()); }
"if"	    { return new_symbol(sym.IF, yytext()); }
"new"       { return new_symbol(sym.NEW, yytext()); }
"print"     { return new_symbol(sym.PRINT, yytext()); }
"read"      { return new_symbol(sym.READ, yytext()); }
"return"    { return new_symbol(sym.RETURN, yytext()); }
"void"      { return new_symbol(sym.VOID, yytext()); }
"extends"   { return new_symbol(sym.EXTENDS, yytext()); }
"continue"  { return new_symbol(sym.CONTINUE, yytext()); }
"for"       { return new_symbol(sym.FOR, yytext()); }
"length"    { return new_symbol(sym.LENGTH, yytext()); }
"switch"    { return new_symbol(sym.SWITCH, yytext()); }
"case"      { return new_symbol(sym.CASE, yytext()); }


"++" 		{ return new_symbol(sym.INC, yytext()); }
"+" 		{ return new_symbol(sym.ADD, yytext()); }

"--"		{ return new_symbol(sym.DEC, yytext()); }
"-" 		{ return new_symbol(sym.SUB, yytext()); }

"*" 		{ return new_symbol(sym.MUL, yytext()); }
"/" 		{ return new_symbol(sym.DIV, yytext()); }
"%" 		{ return new_symbol(sym.MOD, yytext()); }

"==" 		{ return new_symbol(sym.EQ, yytext()); }
"=" 		{ return new_symbol(sym.IS, yytext()); }

"!=" 		{ return new_symbol(sym.NEQ, yytext()); }

">=" 		{ return new_symbol(sym.GE, yytext()); }
">"			{ return new_symbol(sym.GT, yytext()); }

"<=" 		{ return new_symbol(sym.LE, yytext()); }
"<" 		{ return new_symbol(sym.LT, yytext()); }

"&&" 		{ return new_symbol(sym.AND, yytext()); }
"||" 		{ return new_symbol(sym.OR, yytext()); }

";" 		{ return new_symbol(sym.SEMI, yytext()); }
":" 		{ return new_symbol(sym.COLON, yytext()); }
"," 		{ return new_symbol(sym.COMMA, yytext()); }
"." 		{ return new_symbol(sym.DOT, yytext()); }
"(" 		{ return new_symbol(sym.LPAREN, yytext()); }
")" 		{ return new_symbol(sym.RPAREN, yytext()); }
"[" 		{ return new_symbol(sym.LBOX, yytext()); }
"]"			{ return new_symbol(sym.RBOX, yytext()); }
"{" 		{ return new_symbol(sym.LBRACE, yytext()); }
"}"			{ return new_symbol(sym.RBRACE, yytext()); }

"\?" 		{ return new_symbol(sym.QMARK, yytext()); }

"true"					{ return new_symbol(sym.BOOL_CONST, 1); }
"false"					{ return new_symbol(sym.BOOL_CONST, 0); }

[0-9]+   				{ return new_symbol(sym.NUM_CONST, Integer.valueOf(yytext())); }
'.'   					{ return new_symbol(sym.CHAR_CONST, Character.valueOf(yytext().charAt(1))); }

[a-zA-Z][a-zA-Z0-9_]*   { return new_symbol(sym.IDENT, yytext()); }

. { 
	errorBuffer.setLength(0);
	errorLine = yyline + 1;
	errorColumn = yycolumn + 1;
	errorBuffer.append(yytext());
	yybegin(ERROR_STATE);
}

<ERROR_STATE> [a-zA-Z0-9_\t\r\n \(\)\[\]\{\}\;\:\,\.\+\-\*\/\%\=\!\<\>\&\|\?\'\"] {
	// Prepoznat validan karakter - ispisi gresku i nastavi obradu
	String errorText = errorBuffer.toString();
	int line = errorLine;
	int col = errorColumn;
	errorBuffer.setLength(0);
	System.err.println("Leksicka greska (\"" + errorText + "\") u liniji " + line + ", kolona " + col);
	yypushback(1);
	yybegin(YYINITIAL);
}

<ERROR_STATE> . {
	// Jos jedan neprepoznati karakter - dodaj u buffer
	errorBuffer.append(yytext());
}










