cd out
start java -cp jgroups-3.6.20.Final.jar;. Frontend.java
timeout /t 1
FOR /L %%G IN (1,1,1) DO start java -cp jgroups-3.6.20.Final.jar;. Backend.java
timeout /t 5
#start java -cp jgroups-3.6.20.Final.jar;. Seller.java
#start java -cp jgroups-3.6.20.Final.jar;. Buyer.java