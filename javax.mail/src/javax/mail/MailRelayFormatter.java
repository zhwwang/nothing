package javax.mail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class MailRelayFormatter extends Formatter {

	private SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@Override
	public String format(LogRecord record) {
		return "SMTPD" + record.getThreadID() + " - " + calcDate(record.getMillis()) + ": " + record.getMessage() + "\n"; 
	}
	
	private String calcDate(long millisecs)  
    {  
        Date resultdate = new Date(millisecs);  
        return date_format.format(resultdate);  
    }

}
