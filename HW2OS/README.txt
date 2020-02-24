How the internal interface works:

The front end queries a device or sensor for its state. It then compares its state with the state stored at the back end server. 
If the states are different, then it will change its state and record the time the state change was recorded.

How to run the assignment:

In order to run the program, you first need to make sure that all programs have the same port
and each client has the IP address of the server. The IP address and port of the server can be
changed in all client programs by modifying the “SERVER_IP” and “PORT” constants at the top of 
each class. Also make sure that java and make are installed. As well as making sure that the 
server is accepting incoming and outgoing requests on that port and IP. This would ensure that 
the server can receive and send information to the clients. Once that is completed, run the 
command “make” to compile all the files. Then run the files by using the command “java <Filename>”. 
For example, to run User.java, you need to enter the command “java User”. Run the server file first, 
then run all the clients. Each client can be run on any computer or directory. Once all programs are 
running, you should see the menu with options the user can choose, and occasional messages from the 
server to the user program. The messages will differ based on what options were chosen; for example, 
you should not see a message about an intruder if the security mode it set to HOME. Once you are done 
with the program, simply choose option 14 in the menu to exit. This will stop User.java, but the other 
programs will continue running. To stop the other programs from running press Control-C. 