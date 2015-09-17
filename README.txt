This program is for generating daily log file and query CPU usage for a specific CPU in a given time period. 

To generate the log file, change the String start_time to a specific date, then run “java generate” or “java generate DATA_PATH”

>java generate
or
>java generate DATA_PATH

To query CPU use, change the String start_time to the same date used in generate.java, then run “java query” or “java query DATA_PATH”. The console will then wait for the user to input “QUERY IP cpu_id time_start time_end. Time_start time_end”, which should be specified in the format YYYY-MM-DD HH:MM where YYYY is a four digit year, MM is a two digit month (i.e., 01 to 12), DD is the day of the month (i.e., 01 to 31), HH is the hour of the day, and MM is the minute of an hour. Or only type EXIT/exit to exit. If invalid input is detected(bad format, the time queried doesn’t match the recorded time, etc), the system will output warning and let the user to have a try again.

>java query
QUERY 192.168.1.10 1 2014-10-31 00:00 2014-10-31 00:05
CPU1 usage on 192.168.1.10:
(2014-10-31 00:00, 90%), (2014-10-31 00:01, 89%), (2014-10-31 00:02, 87%), (2014-10-31 00:03,  94%) (2014-10-31 00:04, 88%)
>java query DATA_PATH
QUERY 192.168.1.10 1 2014-10-31 00:00 2014-10-31 00:05
CPU1 usage on 192.168.1.10:
(2014-10-31 00:00, 90%), (2014-10-31 00:01, 89%), (2014-10-31 00:02, 87%), (2014-10-31 00:03,  94%) (2014-10-31 00:04, 88%)
>EXIT

The program used technique of parsing data into smaller chunks and storing the most needed data into cache.  The data structure used to store data in cache is Hashtable<String, ArrayList<String>>, the key is the string combination of ip ad cpu_id, the ArrayList stores the usage of that cpu in timestamp’s ascending order. To query the usage of cpu for a certain time period, we only need to get the index for that certain timestamp, which is calculated by getting the difference between it and the most early timestamp recorded in the log file, then dividing it by 60. If the query data is not stored in the chunk file the system is currently reading, the system will initiate the getFileLabel() function to return the label of the chunk file which contains the query data.