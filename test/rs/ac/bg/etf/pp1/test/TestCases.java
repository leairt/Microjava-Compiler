package rs.ac.bg.etf.pp1.test;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import rs.ac.bg.etf.pp1.exc.ErrorCode;

public class TestCases {
	
	// A NIVO
	
	@Test
	public void a001() {
	    execute("a001");
	    assertEquals(false, hasErrors());

	    String[] out = getOutput();
	    assertEquals("5", out[0]);
	    assertEquals("5", out[1]);
	    assertEquals("10", out[2]);
	    assertEquals("A", out[3]);
	}

	@Test
	public void a002() {
	    execute("a002");
	    assertEquals(true, hasErrors());
	}

	@Test
	public void a003() {
	    execute("a003");
	    assertEquals(true, hasErrors());
	}

	@Test
	public void a004() {
	    execute("a004");
	    assertEquals(true, hasErrors());
	}

	@Test
	public void a005() {
	    execute("a005");
	    assertEquals(true, hasErrors());
	}
	
	// TESTOVI PO FAZAMA

	@Test
	public void p000() {
		execute("p000");
		assertEquals(false, hasErrors());
	}

	@Test
	public void p001() {
		execute("p001");
		assertEquals(false, hasErrors());
	}
	
	@Test
	public void p002() {
		execute("p002");
		assertEquals(false, hasErrors());
	}

	@Test	
	public void p003() {
		execute("p003");
		assertEquals(false, hasErrors());
	}

	@Test
	public void p004() {
		execute("p004");
		assertEquals(false, hasErrors());
	}

	@Test
	public void p005() {
		execute("p005");
		assertEquals(false, hasErrors());
		
		String[] out = getOutput(); 				
		assertEquals("1", out[0]);
	}

	@Test
	public void p006() {
		execute("p006");
		assertEquals(false, hasErrors());

		String[] out = getOutput(); 				
		assertEquals("123", out[0]);
	}

	@Test
	public void p007() {
		execute("p007");
		assertEquals(false, hasErrors());

		String[] out = getOutput(); 		
		assertEquals("666", out[0]);
		assertEquals("1", out[1]);
	}

	@Test
	public void p009() {
		execute("p009");
		assertEquals(false, hasErrors());
		
		String[] out = getOutput(); 		
		assertEquals("Lea", out[0]);
		assertEquals("Irt", out[1]);
	}

	@Test
	public void p010() {
		execute("p010");
		assertEquals(false, hasErrors());
		
		String[] out = getOutput(); 		
		assertEquals("5", out[0]);
	}

	@Test
	public void p011() {
		execute("p011");
		assertEquals(false, hasErrors());
		
		String[] out = getOutput(); 		
		assertEquals("4", out[0]);
		assertEquals("13", out[1]);
		assertEquals("28", out[2]);
		assertEquals("5", out[3]);
		assertEquals("2", out[4]);
		assertEquals("11", out[5]);
		assertEquals("17", out[6]);
	}

	@Test
	public void p012() {
		execute("p012");
		assertEquals(false, hasErrors());
		
		String[] out = getOutput(); 		
		assertEquals("55", out[0]);
	}

	@Test
	public void p013() {
		execute("p013");
		assertEquals(false, hasErrors());
		
		String[] out = getOutput(); 		
		assertEquals("-5-5", out[0]);
	}
	
	@Test
	public void p014() {
		execute("p014");
		assertEquals(true, hasErrors());
		assertEquals(true, hasError(ErrorCode.LEXIC_ERROR, 3));		
		assertEquals(true, hasError(ErrorCode.LEXIC_ERROR, 4));	
		assertEquals(true, hasError(ErrorCode.LEXIC_ERROR, 6));	
	}

	@Test
	public void p100() {
		execute("p100");
		assertEquals(true, hasErrors());
		assertEquals(true, hasError(ErrorCode.SYNTAX_ERROR, 3));		
	}

	@Test
	public void p101() {
		execute("p101");
		assertEquals(true, hasErrors());
		assertEquals(true, hasError(ErrorCode.SYNTAX_ERROR, 7));		
	}


	@Test
	public void p200() {
		execute("p200");
		assertEquals(true, hasErrors());
	}

	@Test
	public void p201() {
		execute("p201");
		assertEquals(true, hasErrors());
		assertEquals(true, hasError(ErrorCode.CANNOT_BE_RESOLVED_AS_TYPE, "long"));		
	}

	@Test
	public void p202() {
		execute("p202");
		assertEquals(true, hasErrors());
		assertEquals(true, hasError(ErrorCode.UNDECLARED_DESIGNATOR, 5, "x"));		
	}

	@Test
	public void p203() {
		execute("p203");
		assertEquals(true, hasErrors());
		assertEquals(true, hasError(ErrorCode.DUPLICATED_NAME));		
	}

	@Test
	public void p204() {
		execute("p204");
		assertEquals(true, hasErrors());
		assertEquals(true, hasError(ErrorCode.DUPLICATED_NAME));		
	}

	@Test
	public void p205() {
		execute("p205");
		assertEquals(true, hasErrors());
		assertEquals(true, hasError(ErrorCode.DUPLICATED_NAME));		
	}

	@Test
	public void p206() {
		execute("p206");
		assertEquals(true, hasErrors());
		assertEquals(true, hasError(ErrorCode.CANNOT_BE_RESOLVED_AS_TYPE));		
	}

	
	@Test
	public void test301() {
	    setInput("4\n7\n");
	    execute("test301");
	    assertEquals(false, hasErrors());

	    String[] out = getOutput();
	    assertEquals("9", out[0]);
	    assertEquals("6", out[1]);
	    assertEquals("12", out[2]);
	    assertEquals("b", out[3]);
	    assertEquals("c", out[4]);
	    assertEquals("97334737-", out[5]);
	}
	
	@Test
	public void p310() {
	    execute("p310");
	    assertEquals(false, hasErrors());

	    String[] out = getOutput();
	    assertEquals("507", out[0]);
	}

	@Test
	public void p311() {
	    execute("p311");
	    assertEquals(true, hasErrors());
	}

	@Test
	public void p312() {
	    execute("p312");
	    assertEquals(true, hasErrors());
	}

	@Test
	public void p313() {
	    execute("p313");
	    assertEquals(true, hasErrors());
	}

	@Test
	public void p314() {
	    execute("p314");
	    assertEquals(true, hasErrors());
	}

	@Test
	public void p315() {
	    execute("p315");
	    assertEquals(false, hasErrors());

	    String[] out = getOutput();
	    assertEquals("0", out[0]);
	    assertEquals("3", out[1]);
	    assertEquals("65", out[2]);
	    assertEquals("3", out[3]);
	}

	@Test
	public void p316() {
	    execute("p316");
	    assertEquals(true, hasErrors());
	}


	@Test
	public void p317() {
	    execute("p317");
	    assertEquals(true, hasErrors());
	}

	/* test helper operations */
	
	private TestHelper mj = new TestHelper();

	private void execute(String testID) {
		mj.execute(testID);
	}

	private boolean hasErrors() {
		return mj.hasErrors();
	}

	private boolean hasError(ErrorCode errorCode) {
		return mj.hasError(errorCode);
	}

	private boolean hasError(ErrorCode errorCode, int lineNumber) {
		return mj.hasError(errorCode, lineNumber);
	}

	private boolean hasError(ErrorCode errorCode, int lineNumber, String data) {
		return mj.hasError(errorCode, lineNumber, data);
	}

	private boolean hasError(ErrorCode errorCode, String data) {
		return mj.hasError(errorCode, data);
	}
	
	private String[] getOutput() {
		return mj.getOutput();
	}

	private void setInput(String input) {
		mj.setInput(input);
	}
}
