package cn.com.aboobear.mailrelay.misc;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

public class SystemUtils {

	public static boolean deleteFile(String filepath) {
		if (filepath == null) {
			return false;
		}
		boolean result = false;
		File file = new File(filepath);
		if (file.exists() && file.isFile()) {
			result = file.delete();
		}
		return result;
	}

	public static String Format(DateFormat dateFormat, Date pdate) {
		synchronized (dateFormat) {
			return dateFormat.format(pdate);
		}
	}
}
