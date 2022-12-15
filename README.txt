Execute the "Compile.sh" file
(it will create a folder with all the compiled files)

then execute Run.bat
It will open the frontend server, 3 backend replicas and 1 seller + 1 buyer

If you want to open more
go to the newly created folder "out"

then open the console (cmd)
and run either of these commands

java -cp jgroups-3.6.20.Final.jar;. -Djava.net.preferIPv4Stack=true Frontend.java
java -cp jgroups-3.6.20.Final.jar;. -Djava.net.preferIPv4Stack=true Backend.java
java -cp jgroups-3.6.20.Final.jar;. -Djava.net.preferIPv4Stack=true Seller.java
java -cp jgroups-3.6.20.Final.jar;. -Djava.net.preferIPv4Stack=true Buyer.java

Notice: replace ; with : on linux/macOS