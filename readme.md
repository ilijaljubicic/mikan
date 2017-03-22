# Mikan server 

This server allows connected applications to talk to each other across a network, like a typical Chat App.
Application types include browser apps. The idea is to have large numbers of client Apps, 
sending and receiving JSON messages to each other, all at the same time. 
The server can be used in many different ways, the aim however is for high throughput 
simulations and games to talk to each other. With this server you can setup a versatile 
JSON message "repeater" on your network.

# Overview

To explain the server's capabilities, consider a browser App 
called ClientA, that sends messages to another browser App called ClientB. All messages must be 
valid JSON messages. The word message in this document refers to a valid JSON message.

For the Clients to talk to each other, they first need to connect to the Mikan server using a websocket.
Once connected, ClientA can send messages to the server and have ClientB receive those messages.
The server is a publish-subscribe system. That is, ClientA is publishing messages of a given topic, 
and ClientB receives messages of the topic it has subscribed to.
Any Client can be both publisher and subscriber at the same time. 
The default server behaviour is to automatically publish and subscribe Clients to a default "json" topic.
This means any message can be sent and received without further ado.

### Subscribe

To receive only messages of a specific set of topics, ClientB needs to send a "subscribe" command message to the server 
such as: 

    {"mikanType": "subscribe", topic": [topic1, topic2, ...]}

where "topic" must be an array of string. A Client can subscribe to multiple topics.

### Publish

For ClientA to publish messages to a particular topic, it needs to send the server a "publish" command message 
such as: 

    {"mikanType": "publish", "topic": topicName}

A Client can publish to only one topic at any one time. To change topic 
a Client has to send another message with a new topic name. Both publish and subscribe topics
 remain in effect until a new topic name is received by the server.

### Filter

Another feature of the **Mikan server**, is the ability for the Clients to send the server a filter.
This lets the server filter messages in addition to the topic.
The filter is setup by sending a "filter" command message such as: 

    {"mikanType": "filter", "script": codeString}
     
where "codeString" is a string that contains JavaScript code. The server will run that code on behalf of the Client to filter the messages it wants to receive.

As the target use is high speed and throughput, the server has built-in cluster capabilities. 
In addition, the server can run with a database to record all messages, or without a database (default).


# Details

As can be seen from the [Overview](#overview) there are only 3 command messages that a client can send to the server. 
Each, sets the server into a state specific for that client. The server state for that Client remains 
in effect until a new command message replaces the previous one. Note that these commands messages are 
not published to other Clients. To turn off (i.e. return to default) a 
Client can send a message with an empty value. For example:

    {"mikanType": "subscribe", "topic": []}

This command sets the Client to subscribe to the default topic "json", the catch all topic. 

#### Filtering messages

To limit the number 
of messages received by Clients, the **Mikan server** 
provides in addition to topic subscription a filtering based on JavaScript code. 
The following example illustrates its use. Suppose ClientA is a flight simulator sending large numbers of messages of this type; 

    {"entity": "aircraft", "lat": 12.3, "lon": 45.6}.

ClientB wants to receive all "entity" from the simulator, but only those 
between latitudes 10 and 20 degrees. 
To create a filter for this, ClientB writes a bit of JavaScript code and sends it 
to the server using a "filter" message as described in the [Filter section](#filter). 
For example:

    function filter(data) {
        var obj = JSON.parse(data);
        if(obj.lat > 10 && obj.lat < 20) return true;
        return false;
    }
    
The JavaScript code must have a function called **filter** that takes a parameter, which in our case 
is expected to be a message from the simulator. 
It must also return either **true** if we want to receive the message 
or **false** to stop the message. Using such filtering in combination with topic subscription, a Client can 
fine-tune the messages it receives. A Client can replace the current "filter" by sending a new message.
At present only one "filter" can be active at any given time. To turn this type of filtering off, a Client 
can send a "filter" message with an empty "script" value, such as:

    {"mikanType": "filter", "script": ""}

Although the filter is constructed with JavaScript code, the Client App does not have to be a browser or JavaScript App.

### Server settings

The **Mikan server** settings are found in the **appication.conf** file in the "conf" directory. 
This file can be edited to control various functions, such as the database and the clustering. 

#### Database
The server can be set to record all non-command messages to a database. To enable this set:

    mikan.withdatabase = true
     
The default is **false**. 
If the database is enabled, the [mongoDB](https://docs.mongodb.com/) system needs to be installed. See 
 the [MONGODB manual](https://docs.mongodb.com/manual/) to install mongodb. For example on MacOSX:

    brew install mongodb

Once installed the mongo database server needs to be running. To do this type in a terminal:

    mongod

In the **appication.conf** file add the following (customised to your particular environment):

    mongodb.uri = "mongodb://localhost:27017/orange"

This tells the **Mikan server** how to connect to the database named "orange".
There are two other values that can be edited in application.conf regarding the database.
They are: 

    mongo.collection.accounts = "accounts"
    mongo.collection.clientmsg = "clientmsg"

These tell the **Mikan server** the names of the [mongo collections](https://docs.mongodb.com/manual/core/databases-and-collections/) 
to use to save the user accounts and the messages, respectively. The default collections names are those shown above.

#### Cluster
To customise the server cluster capabilities edit the "cluster" settings in application.conf.
 Basically add more "nodes" as required. For a detail description of setting up a cluster refer to 
 [Cluster Usage](http://doc.akka.io/docs/akka/current/scala/cluster-usage.html).

#### Websocket

The websocket endpoint that a Client should connect to, is: **mikanjson** 

For example launching the **Mikan server** on my home computer using: 

    sbt run
    or
    sbt run -Dhttp.port=9000 
    
launches the server on localhost and port 9000. So a Client needs to connect 
 to the websocket:
 
     ws://localhost:9000/mikanjson

Customise this to suit your particular environment, note there is no need 
to include the port number in production environment.

# Requirements
Must have Java 8 installed.

# Installation and running the server
Download or clone this repository, then unpack the source code to a directory of your choice.

To setup the **Mikan server** for running, type in a terminal:

    sbt dist

This produces a zip file containing all JAR files needed to run the application in the ./target/universal folder.
 You can then copy/distribute this zip file to the system of you choice. Unzip the mikan-1.0-SNAPSHOT.zip, then: 
 
     cd to the ./target/universal/mikan-1.0-SNAPSHOT/bin

The "sbt dist" generates a script called "mikan" to run the server. If need be, set the permission to execute this script:
 
    chmod +x mikan

Then type in a terminal:

    mikan -Dplay.crypto.secret=the-secret-key-generated-by-play

where "the-secret-key-generated-by-play" is the secret key for the application. To generate this key 
type at a terminal:

    sbt playGenerateSecret

This will generate something like: "QteM]rmij6vLn7turh3g/]<Ge:EDz^6xvGZ0Z1<mR<t0EiL?csKdDl_?s2JmJ5Xf".

The server uses this secret key for a number of things, including; signing session cookies and CSRF tokens,
 and built in encryption utilities.

You can also run the **Mikan server** locally using:

    sbt run -Dhttp.port=9000


# Security

If you are running this server on the open internet be aware that currently there are no 
security measures at all. All messages are transmitted in plain text. 
Anyone can connect, send and receive messages. A login step will probably be added later to restrict 
user access. The default server behaviour is "without a database", that means nothing is recorded and a 
 random user "Account" is created for each connection for internal server use.

# Example clients

clientA.html is an example of a browser JavaScript App publishing messages to the **Mikan server**. It 
publishes JSON messages to the topic named "entity".

clientB.html is an example of a browser App subscribing to messages of the topic named "entity".

Similarly, cesiumClientB.html is an example browser App receiving the JSON messages 
from clientA.html. It displays the received messages on the [Cesium 3D globe](https://cesiumjs.org/).


# Status

very experimental







 
 
 










