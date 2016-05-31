package captain.agent;

public class Helpers {

	public static boolean isEmpty(String param) {
		return param == null || param.isEmpty();
	}

	public static boolean isInteger(String param) {
		try {
			Integer.parseInt(param);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean isPositive(String param) {
		try {
			return Integer.parseInt(param) > 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean isLong(String param) {
		try {
			Long.parseLong(param);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}
