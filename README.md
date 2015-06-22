# DistributedSnapchatApplication
final code for distributed file server architecture - consistent cluster state and consensus algorithm - raft


  A cluster based image sharing application that allows to transmit and recieve images across multiple clients. The      application
  designed for high availibiliy, fault tolerance and optimum throughput.

  Technologies used:

  * Network Server: Core Netty
  * Message Passing: Google Protobuf
  * DB Layer: MongoDB
  * Server Side Programming Language: Java
  * Client Side Programming Language: Java
  
  Modules:

  o snapchatproto.Server: It contains the server file that is used to start all the
  worker and manager threads inside the server node
  
  o snapchatproto.monitor: Channel Handlers and Initializers.
  
  o snapchatproto.server.management: It contains the inbound and outbound
  management queues and also the discrete queues implementation. It also
  contains the Heartbeat manager thread to process the heartbeats. 
  
  o snapchatproto.resources: It contains the proto files that are used to
  generate the .java files to build the encoded messages and also decode the
  received messages from the communication channel.
  
  o snapchatproto.server.Queue: It contains the channel queue
  implementations that are used to enqueue the incoming and outgoing data
  messages. 
  
  RaftElectionManager.java : This class acts as the context for the current state
  of the node and also starts the election in case of timeout condition.
  
  JobManager.java : This class is used to receive, forward and process the
  incoming job requests from the client or from the other nodes.
  
  NetworkManager.java: This class is used to process the network requests
  from other nodes or from a different cluster.
  
  LogListManager.java : This class is used to send append requests to other
  nodes and also update the logs in the database based on received append
  request.
  
  snapchatproto.servers.timers: It contains the implementation of the timer
  logic which is a quintessential part of the Raft consensus algorithm.
  
  Server.conf file: This file contains information that is essential for the nodes
  to communicate i.e. the Ip address, Data Ports and Management ports of the
  nodes in the cluster.
    
  Authors:
  Vishwa Desai (vishwadesai2@gmail.com)
  Rutvik Dudhia (Rutvikdudhia@gmail.com)
  Gaurav Buche (Gaurav951990@gmail.com)
  Tanvi Deo (Tanvideo91@gmail.com)
   
