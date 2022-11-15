if [ ! -d out ]
then
	mkdir out
fi

for i in */;
do
    cp $i/* out/
done

cd out
javac *.java

start rmiregistry
sleep 2.5
start java Server.java

exit

