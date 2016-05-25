Captain
-------------
Captain is yet another service discovery implementation based on redis.
Captain sacrifices a little high availability for simplicity and performance.
In most cases, we dont have so many machines as google/amazon.
The possibility of machine crashing is very low, high Availability is not so abviously important yet.
But the market only provides zookeeper/etcd/consul, they are complex, at least much complexer compared with captain.

Architecture
-------------
<img src="screenshot/arch.png" width="600" title="Captain Architecture" />

1. If all captain server or redis shutdown, captain client will keep services information in local memory.
2. If just one captain server shutdown, captain client will sync service information from other captain server.
3. Carefully monitor captain server and redis, recovery quickly, high availability still can be guaranteed.

Internal
------------
1. Service List is saved into redis as sortedset with key equals host:port and score equals ${now + ttl}
2. Expiring Thread will periodically trim the expired items of this sortedset
3. To track the changes, redis keeps a global version and each sub version for every service list.
4. Client will periodically check the global version, If the global version changed, client will check all version of dependent services, if any dependent service changes, client will reload the changed service list.
5. Client interact with captain server with http api only

API
-----------
1. keep service /api/service/keep?name=sample&host=localhost&port=6000&ttl=30
2. cancel service /api/service/cancel?name=sample&host=localhost&port=6000
3. global version check /api/service/dirty?version=${client local version}
4. get service version /api/service/version?name=sample1&name=sample2
5. get service list /api/service/set?name=sample

Install Captain Server
---------------------
```bash
install redis
install java8
install maven

git clone github.com/pyloque/captain.git
cd captain
mvn package
java -jar target/captain.jar
java -jar target/captain.jar 6789  # http bind port
java -jar target/captain.jar 6789 localhost:6379 # specify redis url
java -jar target/captain.jar 6789 localhost:6379 1000 # specify expiring watch interval for 1000ms
java -jar target/captain.jar 6789 localhost:6379 0 # runs in readonly mode with expiring watch not started

open web ui
http://localhost:6789
```

Web UI
------------------------
<img src="screenshot/all_services.png" width="600" title="All Services" />
<img src="screenshot/service_list.png" width="600" title="Service List" />

Client SDK
------------------------
1. Python Client https://github.com/pyloque/pycaptain
2. Java Client https://github.com/pyloque/captain-java
3. Golang Client https://github.com/pyloque/gocaptain
