UPDATE: Backend Server can't be replicated as the .getState() method from JChannel doesn't work as indicated.
I tried to work around it with the .setState() method from the ReceiverAdapter class, but it didn't work either.
Therefore, only one replica can be launched

Execute the "Compile.sh" file
(it will create a folder with all the compiled files)

then execute Run_Windows.bat or Run_Linux.sh (which may not work properly as it was tested on Windows)
It will open the frontend server, 1 backend replica and 1 seller + 1 buyer
(for more replicas use below command or replace in script in side the for the end number)

If you want to open more
go to the newly created folder "out"

then open the console (cmd)
and run either of these commands

java -cp jgroups-3.6.20.Final.jar;. -Djava.net.preferIPv4Stack=true Frontend.java
java -cp jgroups-3.6.20.Final.jar;. -Djava.net.preferIPv4Stack=true Backend.java
java -cp jgroups-3.6.20.Final.jar;. -Djava.net.preferIPv4Stack=true Seller.java
java -cp jgroups-3.6.20.Final.jar;. -Djava.net.preferIPv4Stack=true Buyer.java

Notice: replace ; with : on linux/macOS