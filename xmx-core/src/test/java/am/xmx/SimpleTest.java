package am.xmx;

import org.junit.Test;

import com.google.gson.Gson;

public class SimpleTest {
	
	@Test
	public void foo() {
		Gson gson = new Gson();
//		String str = gson.toJson(50000);
//		System.err.println(str);
//		Object aaa = gson.fromJson("5000", Integer.class);
		System.err.println(gson.fromJson("null", Integer.class));
	}
	

}
