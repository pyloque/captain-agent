Captain
-------------
Captain is yet another service discovery implementation based on redis.
Captain sacrifices a little high availability for simplicity and performance.
In most companies, we dont have so many machines as google, amazon, alibaba etc.
The possibility of machine crashing is very low, high Availability is not so abviously important yet.
But the market only provides zookeeper/etcd/consul, they are complex, at least much complexer compared with captain.

Architecture
-------------
!(Captain Architecture)[screenshot/arch.png]

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

Use Captain Java Client
-----------------------
```
git clone github.com/pyloque/captain-java.git

#Service1
import captain.ShadisClient

public class Service1 {

    public static void main(String[] args) throws Exception {
        ShadisClient client = new ShadisClient("localhost", 6789);
        client.provide("service1", new ServiceItem("localhost", 6000)).start();
        # jvm hangs here until
        # client.stop()
    }
}
#Service2
import captain.ShadisClient

public class Service2 {

    public static void main(String[] args) throws Exception {
        ShadisClient client = new ShadisClient("localhost", 6789);
        client.provide("service2", new ServiceItem("localhost", 6001)).start();
        # jvm hangs here until
        # client.stop()
    }
}
#Service3
import captain.ShadisClient

public class ShadisClientTest {

    public static void main(String[] args) throws Exception {
        ShadisClient client = new ShadisClient("localhost", 6789);
        client.watch("service1", "service2").provide("service3", new ServiceItem("localhost", 6002)).start();
        Thread.sleep(1000);
        System.out.println(client.select("service1").urlRoot());
        System.out.println(client.select("service2").urlRoot());
        # jvm hangs here until
        # client.stop()
    }
}

#Service4
import captain.ShadisClient

public class ShadisClientTest {

    public static void main(String[] args) throws Exception {
        ShadisClient client = new ShadisClient("localhost", 6789);
        client.watch("service1", "service2", "service3").start();
        Thread.sleep(1000);
        System.out.println(client.select("service1").urlRoot());
        System.out.println(client.select("service2").urlRoot());
        System.out.println(client.select("service3").urlRoot());
        # jvm hangs here until
        # client.stop()
    }
}
```
