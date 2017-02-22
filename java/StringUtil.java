package ftptest;

public class StringUtil {

	public static boolean isNullOrEmpty(String str) {
		if (null == str) {
			return true;
		}
		if ("".equals(str)) {
			return true;
		}
		if ("".equals(str.replaceAll("[\\s]", ""))) {
			return true;
		}
		return false;
	}

	public static boolean isNotNullNorEmpty(String str) {
		return !isNullOrEmpty(str);
	}

}
