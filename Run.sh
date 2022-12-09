jarFile="jgroups-3.6.20.Final.jar"
if [ ! -d out ]
then
	mkdir out
fi

cp $jarFile out/

for i in */;
do
    cp $i/* out/
done

cd out
javac -cp $jarFile *.java

#start java -cp $jarFile;. Frontend.java

exit

