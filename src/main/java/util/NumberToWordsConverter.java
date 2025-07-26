package util;
/**
 * @author Christopher Faseun
 *
 */

public class NumberToWordsConverter{
	/** Need to read this from property file **/
    final private static String    currency="NGN";
    final private static String    currencyDesc="Naira";
    
	final private  static String[] units = {
		                                    "Zero","One","Two","Three","Four",
											"Five","Six","Seven","Eight","Nine","Ten",
											"Eleven","Twelve","Thirteen","Fourteen","Fifteen",
											"Sixteen","Seventeen","Eighteen","Nineteen"
										   };
	final private static String[] tens =  {
											"","","Twenty","Thirty","Forty","Fifty",
										   "Sixty","Seventy","Eighty","Ninety"
										  };
	
	public NumberToWordsConverter(){}

	public static String convert_(Integer i){
		if( i < 20)  return units[i];
		if( i < 100) return tens[i/10] + ((i % 10 > 0)? " " + convert(i % 10):"");
		if( i < 1000) return units[i/100] + " Hundred" + ((i % 100 > 0)?" and " + convert(i % 100):"");
		if( i < 1000000) return convert(i / 1000) + " Thousand " + ((i % 1000 > 0)? " " + convert(i % 1000):"") ;
		return convert(i / 1000000) + " Million " + ((i % 1000000 > 0)? " " + convert(i % 1000000):"") ;
	}
	
	public static String convert(Integer i) {
		if( i < 20)   return units[i];
		if( i < 100)  return tens[i/10] + ((i % 10 > 0)? " " + convert(i % 10):"");
		if( i < 1000) return units[i/100] + " Hundred" + ((i % 100 > 0)?" and " + convert(i % 100):"");
		if( i < 1000000){
			String result=convert(i / 1000) + " Thousand ";
			
			if((i % 1000 > 99)){
				result+=", " + convert(i % 1000);
			}else if((i % 1000 > 0)){
				result+="and  " + convert(i % 1000);
			}else{
				result+="";
			}
			
			return result;
		}else{			
			String result=convert(i / 1000000) + " Million ";
			
			if((i % 1000000 > 999)){
				result+=", " + convert(i % 1000000);
			}else if((i % 1000000 > 99)){
				result+=", " + convert(i % 1000000);
			}else if((i % 1000000 > 0)){
				result+=" and  " + convert(i % 1000000);
			}else{
				result+="";
			}
			
			return result;
		}		
	}	
	
	public String convertIntegerValue(int ii){
		return convert(ii)+ " " + currencyDesc;
	}
	
	public String convertStringValue(String ii){
		return convertDoubleValue(Double.parseDouble(ii));
	}
	
	public String convertDoubleValue(double ii){
		int i=0;
		int j=0;
		String result="";		
		String number=new java.text.DecimalFormat("##.00").format(ii);
		
		i=Integer.parseInt(number.substring(0,number.indexOf(".")));				
		j=Integer.parseInt(number.substring(number.indexOf(".")+1));
		String koboEquiv="";
		
		result=convert(i) + " " + currencyDesc;
		
		if(j>0){
			if(i>0){
				result+=" and ";
			}
			
			koboEquiv=convert(j)  + " Kobo";
		}	
		
		return result+ koboEquiv + " Only";
	}
	
	public static void main(String args[]){
		System.out.println("" + new NumberToWordsConverter().convertDoubleValue(1201050901.100));
	}
}