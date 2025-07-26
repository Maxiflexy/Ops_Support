package util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class DtTm {
	final static DecimalFormat fmt = new DecimalFormat("##0.0000");
	final public static String getTm(){

		return ""+System.currentTimeMillis();

	}

	final public static String getTmDiff(String endStr, String startStr){

		long taskStart = 0l;
		try {
			taskStart = Long.parseLong(startStr);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		long taskStart1 = 0l;
		try {
			taskStart1 = Long.parseLong(endStr);
		} catch (NumberFormatException e) {

			e.printStackTrace();
		}

		double taskTat = (taskStart1 - taskStart)/1000.00;

		if (fmt == null){
			return (new DecimalFormat("##0.0000")).format(taskTat);
		}{
			return fmt.format(taskTat);

		}
	}

	final public static String getTmDiff(String tmStr){

		long taskStart = 0l;

		try {
			taskStart = Long.parseLong(tmStr);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		double taskTat = (System.currentTimeMillis() - taskStart)/1000.00;

		if (fmt == null){
			return (new DecimalFormat("##0.0000")).format(taskTat);
		}{
			return fmt.format(taskTat);

		}



	}


	public static String getAcceptanceDateTime(int diff) {
		Calendar cal= Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.SECOND, diff);
		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(cal.getTime());

	}

	public static String formatTime(Date date) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return format.format(date);
	}


	public static String getCurrentDateTime() {
		Calendar cal= Calendar.getInstance();
		cal.setTime(new Date());
		//cal.add(Calendar.SECOND, 85);
		// cal.add(Calendar.SECOND, 67);
		// cal.add(Calendar.SECOND, 14);
		// cal.add(Calendar.SECOND, 66);
		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(cal.getTime());

	}


	public static String getCurrentDateTime(int diff) {
		Calendar cal= Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.SECOND, diff);
		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(cal.getTime());


	}



	public static String getCurrentDateTime2() {
		Calendar cal= Calendar.getInstance();
		cal.setTime(new Date());
		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(cal.getTime());

	}


	public static String getAcceptanceDateTime() {
		Calendar cal= Calendar.getInstance();
		cal.setTime(new Date());
		//cal.add(Calendar.SECOND, 85);
		//cal.add(Calendar.SECOND, 85);
		//cal.add(Calendar.SECOND, 70);
		cal.add(Calendar.SECOND, 65);
		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(cal.getTime());

	}


	public static String getCurrentDate() {

		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyyMMdd", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(new Date());

	}





	public static String getCurrentTime() {

		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("HHmmssSSSSSS", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(new Date());

	}

	public static boolean isValidDateTime(String date, String format) {

		DateTimeFormatter dt = DateTimeFormatter.ofPattern(format, Locale.US)
				.withResolverStyle(ResolverStyle.STRICT);
		try {
			dt.parse(date);
		} catch (DateTimeParseException e) {
			return false;
		}
		return true;

	}




	public static void main(String[] args) {

		String ref="20220218GH100414153006";
		String id="GH1004";

		/*
		 * int idLen = 6;
		 *
		 * try { idLen = id.trim().length(); } catch (RuntimeException e1) {
		 *
		 * } catch (Exception e1) { // TODO Auto-generated catch block
		 * e1.printStackTrace(); //LOG.error(e1.getMessage(),e1); }
		 *
		 * String idStr=ref.substring(8,8+idLen); System.out.println(idStr);
		 */
		System.out.println(getCurrentDateTime());

	}

}