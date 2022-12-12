Execute the "Compile.sh" file
(it will create a folder with all the compiled files)

then execute Run.bat
It will open the frontend server, 3 backend replicas and 1 seller + 1 buyer

If you want to open more
go to the newly created folder "out"

then open the console (cmd)
and run either of these commands

java -cp jgroups-3.6.20.Final.jar;. Frontend.java
java -cp jgroups-3.6.20.Final.jar;. Backend.java
java -cp jgroups-3.6.20.Final.jar;. Seller.java
java -cp jgroups-3.6.20.Final.jar;. Buyer.java

Notice: replace ; with : on linux/macOS