xpanes --log=output/server -s -c "ssh -t {} 'cd hw2 && rm -f file.txt && source setup.sh && java -jar target/hw2-1.0-SNAPSHOT.jar server'" dc0{1..8}.utdallas.edu
xpanes --log=output/client -s -c "ssh -t {} 'cd hw2 && source setup_case2.sh && java -jar target/hw2-1.0-SNAPSHOT.jar client'" dc1{2..6}.utdallas.edu
