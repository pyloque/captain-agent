Captain
-------------
Captain is yet another service discovery implementation based on redis.
Captain sacrifices a little high availability for simplicity and performance.
In most companies, we dont have so many machines as google, amazon, alibaba etc.
The possibility of machine crashing is very low, high Availability is not so abviously important yet.
But the market only provides zookeeper/etcd/consul, they are complex, at least much complexer compared with captain.

Architecture
-------------
![Captain Architecture](screenshot/arch.png)

1. If all captain server or redis shutdown, captain client will keep services information in local memory.
2. If just one captain server shutdown, captain client will sync service information from other captain server.
3. Carefully monitor captain server and redis, recovery quickly, high availability still can be guaranteed.

Install Captain Server
---------------------
```
install redis
install java8
install maven

git clone github.com/pyloque/captain.git
cd captain
mvn package
java -jar target/captain.jar
java -jar target/captain.jar 6789  # http bind port
java -jar target/captain.jar 6789 localhost 6379 # specify redis url

open web ui
http://localhost:6789
```

Web UI
------------------------
![All Services](screenshot/all_services.png)
![Service List](screenshot/service_list.png)

Client SDK
------------------------
1. Python Client https://github.com/pyloque/pycaptain
2. Java Client https://github.com/pyloque/captain-java
3. Golang Client https://github.com/pyloque/gocaptain
