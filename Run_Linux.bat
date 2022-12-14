cd out
start java -cp jgroups-3.6.20.Final.jar:.  -Djava.net.preferIPv4Stack=true Frontend.java
timeout /t 1
FOR /L %%G IN (1,1,1) DO start java -cp jgroups-3.6.20.Final.jar:. -Djava.net.preferIPv4Stack=true Backend.java
timeout /t 5
start java -cp jgroups-3.6.20.Final.jar:. -Djava.net.preferIPv4Stack=true Seller.java
start java -cp jgroups-3.6.20.Final.jar:. -Djava.net.preferIPv4Stack=true Buyer.java