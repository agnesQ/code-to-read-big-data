import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;



public class generate{
    
    static int cpu_id = 0;
    static String IP = "192.168.";
    static int ip_suff1 = 0;
    static int ip_suff2 = 0;
    static String start_time = "31/10/2014 00:00";
    
    
    //=== generate a daily log file for the cpu usage with the given start_time
    public static void main(String args[]){
        String filename = "log.txt";
        if(args.length != 0)
            filename = args[0] + filename;
        writeToFile(filename);
        //System.out.println("done the generating process!");
        }
    
    public static void writeToFile(String filename){
        try {
            
			String content = "timestamp IP cpu_id usage";
            
			File file = new File(filename);
            
            if (!file.exists())
				file.createNewFile();
			
            
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
            bw.newLine();
            
            
            int count_time = 0, count_server = 0;
            
            
            while(count_time < 1440){//60*24 for one day
                
                String timestamp = unixTime(count_time);
                count_time++;
                
                String ip_input = IP+ip_suff1+"."+ip_suff2;
                Random r = new Random();
                int usage = r.nextInt(100);
                content = timestamp+" "+ip_input+" "+cpu_id+" "+usage;
                bw.write(content);
                bw.newLine();
                
                while(count_server < 1999){ // form 0 - 1999, tot 2000
                    count_server++;

                    if(cpu_id == 1){
                        ip_suff2++;
                        cpu_id = 0;
                        if(ip_suff2 == 256){ // if reach 256 then carry a 1 to the next digit
                            ip_suff2 = 0;
                            ip_suff1++;
                        }
                    }
                    else
                        cpu_id++;
                    
                    ip_input = IP+ip_suff1+"."+ip_suff2;
                    r = new Random();
                    usage = r.nextInt(100);
                    content = timestamp+" "+ip_input+" "+cpu_id+" "+usage;
                    
                    bw.write(content);
                    bw.newLine();
                }
                count_server = 0;
                ip_suff1 = 0;
                ip_suff2 = 0;
                cpu_id = 0;
            }
            //System.out.println(count_time + ","+count_server);
			bw.close();
            
        }catch (IOException e) {
			e.printStackTrace();
            System.exit(1);
		}
    }
    
    //=== convert to unixtime
    public static String unixTime(int count_time){
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String unixT = null;
        
        try {
            Date parsedDate = sdf.parse(start_time);
            Calendar cal  = Calendar.getInstance();
            cal.setTime(parsedDate);
            cal.add(Calendar.MINUTE, count_time);
            
            unixT = "" + ((long)cal.getTime().getTime()/1000L);
        }
        catch (ParseException e) {
            ;
        }
        return unixT;
    }
    
    
}

