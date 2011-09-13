package buildhelper002.custom;

import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;

import buildhelper002.main.MainTreeClass;

public class CustomTreeClassTest extends TestCase {

	public void testRead() throws Exception {
		InputStream in = getClass().getResourceAsStream("mainTreeTest.txt");
		String expected = IOUtils.toString(in);
		String actual = new MainTreeClass().read();
		assertEquals(expected, actual);
	}
}
