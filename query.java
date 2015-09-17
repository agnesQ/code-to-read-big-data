import java.util.*;
import java.io.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.nio.charset.Charset;


public class query{
    
    // hashtable would be better for multi thread, but hashmap would be faster, just not sure if need to upgrad to multi thread, so try to use hashmap first
    static HashMap<String,ArrayList<String>> table = new HashMap<String,ArrayList<String>>();
    static int currFileLabel = 0;
    static long[] timeStart = new long[4];
    static int linesPerFile = 720000;
    static String start_time = "2014-10-31 00:00";
    
    public static void main(String args[]){
        
        long startTime = 0;
        
        String IP, cpu, starttime, endtime;
        boolean keepRunning = true;
        
        String filename = "log.txt";
        if(args.length != 0)
            filename = args[0] + filename;
        
        prepareFile(filename);
        
        Console console = System.console(); // open console
        if (console == null)
        {
            System.err.println("No console!");
            System.exit(1);
        }
        
        while (keepRunning)// wait for the users input
        {
            String input =  console.readLine();
            if ("exit".equals(input) || "EXIT".equals(input))
            {
                keepRunning = false;
            }
            else
            {
                startTime = System.currentTimeMillis();
                String[] tmp = input.split(" ");
                if(tmp.length != 7){ // check the validity of the input
                    System.out.println("invalid input!");
                    continue;
                }

                    
                IP = tmp[1];
                cpu = tmp[2];
                
                starttime = tmp[3]+" "+tmp[4];
                endtime = tmp[5]+" "+tmp[6];
                
                if(!validate(IP, cpu, starttime, endtime)){ // check the validity of the input
                
                    System.out.println("invalid input!");
                    continue;
                }
                
                String st = null;
                String ed = null;
                
                try {
                    st = convertDatetoUnix(starttime);
                    ed = convertDatetoUnix(endtime);
                }
                catch (ParseException e) {
                    ;
                }
                
                //System.out.println(st + " "+ timeStart[0] + " "+ ed + " " + (timeStart[0]+86340));//86340 = 60*60*24-60
                
                if(Long.parseLong(st)< timeStart[0]){
                    st = ""+timeStart[0];
                    starttime = start_time;
                }
                if(Long.parseLong(ed) > timeStart[0]+86400)
                    ed = ""+(timeStart[0]+86400);
                    
                int timediff = (int)(Long.parseLong(st) - timeStart[0])/60;
                int FileLabel = getFileLabel(IP,cpu,timediff,-1);
                
                //System.out.println(currFileLabel);
                //System.out.println(FileLabel);
                
                if(currFileLabel != FileLabel){ // if the current file dosent contain the data for the query timestamp, change file, reload data into hashtable
                    //System.out.println("need to change table " + FileLabel);
                    prepareForCache(FileLabel + ".txt");
                    currFileLabel = FileLabel;
                }
                ArrayList<String> result = getMultiple(IP+" "+cpu,st,ed);
                try {
                    printResult(result,IP,cpu,starttime);
                }
                catch (ParseException e) {
                    ;
                }
            }
        }
        //long endTime   = System.currentTimeMillis();
        //long totalTime = endTime - startTime;
        //System.out.println(totalTime/1000);
    }
    
    //== check the validity of the input data
    public static boolean validate(String IP,String cpu, String starttime, String endtime){
        String[] tmp = IP.split("\\.");
        if(tmp.length != 4)
            return false;
    
        if(!cpu.equals("1") && !cpu.equals("0"))
            return false;
        
        
        return isTimeStampValid(starttime)&&isTimeStampValid(endtime);
        
    }
    
    //== check the validity of the input timestamp
    public static boolean isTimeStampValid(String inputString)
    {
        SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        try{
            format.parse(inputString);
            return true;
        }
        catch(ParseException e)
        {
            return false;
        }
    }
    
    //=== print result after the getMultiple funciton is called
    public static void printResult(ArrayList<String> result, String IP, String cpu, String starttime)throws ParseException{
        
        System.out.println("CPU"+cpu+" usage on "+IP+":");
        
        if(result.size() == 0 || result == null){
            System.out.println("there's no such record, check your input again!");
            return;
        }
        
        SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date parsedDate = dateFormat.parse(starttime);
        Calendar cal  = Calendar.getInstance();
        cal.setTime(parsedDate);
        starttime = ""+dateFormat.format(cal.getTime());
        
        for(int i = 0; i < result.size(); i++){
            System.out.print("("+starttime+", "+result.get(i)+"%)");
            
            
            parsedDate = dateFormat.parse(starttime);
            cal  = Calendar.getInstance();
            cal.setTime(parsedDate);
            cal.add(Calendar.MINUTE, 1);
            starttime = ""+dateFormat.format(cal.getTime());
            
            if(i < result.size()-1)
                System.out.print(",");
        }
        System.out.println();
    }
    
    //=== check if close and delete all the tmp files
    /*
    public static void Exit(int range){
        
        for(int i = 1; i <= range; i++){
            String path = i+".txt";
            try {
                Files.delete(path);
            } catch (NoSuchFileException x) {
                System.err.format("%s: no such" + " file or directory%n", path);
            } catch (DirectoryNotEmptyException x) {
                System.err.format("%s not empty%n", path);
            } catch (IOException x) {
                // File permission problems are caught here.
                System.err.println(x);
            }

        }
        
    }
    */
    
    //==== convert input from string "yyyy-MM-dd HH:mm" to date to unix time
    public static String convertDatetoUnix(String input) throws ParseException {
        java.util.Date parsedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm")
        .parse(input);
        Calendar cal  = Calendar.getInstance();
        cal.setTime(parsedDate);
        String unixT = "" + ((long)cal.getTime().getTime()/1000L);
        return unixT;
    }
    
    
    //==== fetch all the usage data with their timestamps within starttime and endtime
    public static ArrayList<String> getMultiple(String ipcpu, String starttime, String endtime){
        ArrayList<String> list = new ArrayList<String>();
        long st = Long.parseLong(starttime);
        long ed = Long.parseLong(endtime);
        
        while(st<ed){
            String tt = get(ipcpu,""+st);
            if(tt != null && !tt.equals("changefile"))
                list.add(tt);
            else if(tt != null && tt.equals("changefile")){
                currFileLabel++;
                prepareForCache(currFileLabel + ".txt");
                continue;
            }
            st += 60; // in unix time sys, there is a 60L between each minute
        }
        return list;
    }
    
    //=== one time fetch from hashtable
    public static String get(String ipcpu, String time){
        int index = getIndex(timeStart[currFileLabel-1],time);
        ArrayList<String> list = new ArrayList<String>();
        
        if(table.containsKey(ipcpu))
            list = table.get(ipcpu);
        else
            return null;
        
        if(index < list.size())
            return list.get(index);
        else
            return "changefile";
    }
    
    //=== return the corresponding index of the a certain timestamp in the arrayList (value in hashtable with the ipcpu string as key), String end --> certain timestamp, long start --> the first timestamp in a temp file
    public static int getIndex(long start, String end){
        return (int)(Long.parseLong(end) - start)/60;
    }
    
    //=== return the label of the temp file which contains the usage data of a certain time
    public static int getFileLabel(String IP, String cpu, int timediff, int line){
        //based on the ip+cpu, find out in which file the data is stored
        if(line == -1){
            
            String[] tmp = IP.split("\\.");
            line = Integer.parseInt(tmp[3]) + Integer.parseInt(tmp[2])*256*2 + Integer.parseInt(cpu) + 2000 * timediff;
            
        }
        return line/linesPerFile + 1;
    }
    
    //=== parse the "log.txt" into 4 temp files(by default), based on which range the timestamp is in 00:00-05:59 06:00-11:59 12:00-17:59 18:00-23:59
    public static void prepareFile(String filename){
        
        try{
            //create temp files
            File f1 = new File("1.txt");
            if (!f1.exists())
				f1.createNewFile();
            
            File f2 = new File("2.txt");
            if (!f2.exists())
				f2.createNewFile();
            
            File f3 = new File("3.txt");
            if (!f3.exists())
				f3.createNewFile();
            
            
            File f4 = new File("4.txt");
            if (!f4.exists())
				f4.createNewFile();
            
        }catch(IOException e){
            e.printStackTrace();
        }
        
        
        try{
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line = null;
            int lineNumber = 0;
            int changeWriteCount = 0;
            line = reader.readLine(); // get rid of the first line in "log.txt"
            FileWriter fw = null;
            BufferedWriter bw = null;
            
            while ((line = reader.readLine()) != null) {
                
                if(changeWriteCount == 0){
                    int index = getFileLabel("","",0,lineNumber); // mod the curr line number to tot line number
                    filename = index+".txt";
                    fw = new FileWriter(filename);
                    bw = new BufferedWriter(fw);
                    timeStart[index-1] = Long.parseLong(line.substring(0,10)); // record the first timestamp in that file to String[file_index] timeStart
                }
                
                lineNumber++;
                changeWriteCount++;
                
                bw.write(line);
                bw.newLine();
                
                if(changeWriteCount >= linesPerFile){ // change to another file and continue to write
                    bw.close();
                    fw.close();
                    changeWriteCount = 0;
                }
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            System.exit(1);
        }
        
        
        
    }
    
    //=== read in the content of a certain file into cache, using hashtable
    // the timestamp determines which file to be opened
    public static void prepareForCache(String filename){
        String[] tmp = new String[4];
        table.clear();
        System.gc();
        //Charset charset = Charset.forName("US-ASCII");
        try{
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line = null;
            int linenumber = 0;
            
            while ((line = reader.readLine()) != null) {
                linenumber++;
                tmp = line.split(" ");
                // arrange the timestamp in ascending order into the arraylist
                // search key is the ipcpu string
                if(table.containsKey(tmp[1]+" "+tmp[2])){
                    ArrayList<String> list = table.get(tmp[1]+" "+tmp[2]);
                    list.add(tmp[3]);
                    table.put(tmp[1]+" "+tmp[2],list);
                }
                else
                {
                    ArrayList<String> list = new ArrayList<String>();
                    list.add(tmp[3]);
                    table.put(tmp[1]+" "+tmp[2],list);
                }
            }
            
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            System.exit(1);
        }
    }
}






