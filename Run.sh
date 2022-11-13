
for i in */;
do
    cp $i/* out/
done
cd out
javac *.java

start rmiregistry
sleep 2
start java Server.java

exit

