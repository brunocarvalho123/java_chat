Made by Bruno Carvalho up201508043

------------------------------------------------------------------------------

To compile:
	
	javac ChatServer.java
	javac ChatClient.java

------------------------------------------------------------------------------

To run:

	java ChatServer (port number)
	java ChatClient (server) (port number)

------------------------------------------------------------------------------

Commands:

	/nick (nickname) 	-> sets your nickname
	/join (room name) 	-> joins a room
	/leave	 		-> leaves the room
	/bye			-> leaves the chat
	/priv (name) (message)  -> sends private message
	/print users		-> prints all the users
	/print rooms		-> prints all the rooms
	/print x		-> prints aditional info

------------------------------------------------------------------------------

Important:

	You can only join a room if you have set a nickname

	You can send private messages without a set room

	You can only send group messages if you are in a room
