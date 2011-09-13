package buildhelper002.custom;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class CustomTreeClass 
{
	public String read() throws IOException {
    	InputStream in = getClass().getResourceAsStream("customTree.txt");
    	return IOUtils.toString(in);
	}
}
