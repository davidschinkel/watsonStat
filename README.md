# watsonStat
A small analyzer for time tracked with watson (https://github.com/TailorDev/Watson)

It currently supports two operations
- balance
- list

The operation *balance* returns the balance for the given time interval. That means, the difference between the time you should work an the time you tracked work. The operation *list* returns a list of the daily balances as a comma-separated list. For every day all the projects, tags and identifier are printed out too.

Usage: watsonStat options_list
Arguments: 
- operation -> Operation to perform on the data { Value should be one of [balance, list] }
- frames -> Path of frames file { String }
- start date -> start date for evaluation, format YYYY-mm-dd { String }
- end date -> end date of evaluation, format YYYY-mm-dd, now if not specified (optional) { String }

Options: 
- --mondayWorkingHours [8.0] -> Working hours on monday { Double }
- --tuesdayWorkingHours [8.0] -> Working hours on tuesday { Double }
- --wednesdayWorkingHours [8.0] -> Working hours on wednesday { Double }
- --thursdayWorkingHours [8.0] -> Working hours on thursday { Double }
- --fridayWorkingHours [8.0] -> Working hours on friday { Double }
- --saturdayWorkingHours [0.0] -> Working hours on saturday { Double }
- --sundayWorkingHours [0.0] -> Working hours on sunday { Double }
- --help, -h -> Usage info 
    
If you have any remarks feel free to write me, create a merge request or an issue.
