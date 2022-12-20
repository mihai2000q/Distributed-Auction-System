cd out
start java -cp jgroups-3.6.20.Final.jar:.  -Djava.net.preferIPv4Stack=true Frontend.java
echo Running Frontend Server...
sleep 1
for i in $(seq 1 1);
do
    echo "Running Backend Server $i..."
    start java -cp jgroups-3.6.20.Final.jar:. -Djava.net.preferIPv4Stack=true Backend.java
done
sleep 5
start java -cp jgroups-3.6.20.Final.jar:. -Djava.net.preferIPv4Stack=true Seller.java
echo Running Seller Client...
sleep 5
start java -cp jgroups-3.6.20.Final.jar:. -Djava.net.preferIPv4Stack=true Buyer.java
echo Running Buyer Client...
sleep 1