cd C:\Users\sreek\eclipse-workspace\AOS_PROJ3\src
rm *.class
javac *.java
Start-Process java -ArgumentList('metaserver',0)
Start-Process java -ArgumentList('Client',0)
Start-Process java -ArgumentList('Client',1)
Start-Process java -ArgumentList('Server',0)
Start-Process java -ArgumentList('Server',1)
Start-Process java -ArgumentList('Server',2)
Start-Process java -ArgumentList('Server',3)
Start-Process java -ArgumentList('Server',4)
Start-Process java -ArgumentList('Server',5)