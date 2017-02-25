package cn.com.aboobear.mailrelay.misc;

public class Constants {

	public static int START = 0;
	public static int INPROGRESS = 1;
	public static int DONE = 2;
	public static int FAILURE = 3;
	public static int RETRY = 4;
	public static int SOFT_FAILURE = 5;
	public static int FORBIDDEN = 6;
	public static int REJECT = 7;
	public static int WAIT_AUDIT = 8;
	public static int WAIT_APPROVE = 9;
	public static int ARCHIVE = 10;
	public static int DELETE = 11;
	public static int WAIT_SCAN = 12;
	
	public static int MAIL_NORMAL = 0;
	public static int MAIL_SPAM = 1;
	public static int MAIL_VIRUS = 2;
	public static int MAIL_WAIT_SCAN = 3;
	public static int MAIL_OTHER = 4;
}
