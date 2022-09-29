# Compile
## Client
On the project root run:
```
javac -d bin/ -sourcepath src/ ./src/seitchizcliente/mainCliente.java
```
## Server
On the project root run:
```
javac -d bin/ -sourcepath src/ ./src/seitchizserver/main/Main.java
```

# Execute
## Client
With jar file:
```
java -Djava.security.manager -Djava.security.policy==user.policy -jar client.jar <server_ip>:<server_port> <username> <password>
```
Without jar file:
```
java -classpath bin/ -Djava.security.manager -Djava.security.policy==user.policy seitchizcliente.mainCliente <server_ip>:<server_port> <username>
```
## Server
With jar file:
```
java -Djava.security.manager -Djava.security.policy==server.policy -jar server.jar <port_listenning>
```
Without jar file:
```
javac -d bin/ -sourcepath src/ ./src/seitchizserver/main/Main.java
```

# Security Details
## Client
With the policy file given you can only connected on port 45678
## Server
With the policy file given you can only listen on port 45678

# Limitations
According to the project specifications we found no limitations
