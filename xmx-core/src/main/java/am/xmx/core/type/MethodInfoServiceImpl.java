package am.xmx.core.type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodInfoServiceImpl implements IMethodInfoService {

	@Override
	public String getMethodNameTypeSignature(Method m) {
		StringBuilder sb = new StringBuilder();
		int mod = m.getModifiers() & Modifier.methodModifiers();
		if (mod != 0) {
			sb.append(Modifier.toString(mod)).append(' ');
		}
		sb.append(m.getReturnType().getName());
		sb.append(' ').append(m.getName());
		return sb.toString();
	}

	@Override
	public String[] getMethodParameters(Method m) {
		Class<?>[] params = m.getParameterTypes();
		String[] ret = new String[params.length];
		for (int j = 0; j < params.length; j++) {
			ret[j] = params[j].getName();
		}
		return ret;
	}
}
