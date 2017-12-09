import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

class User {
	int port;
	String nome;
	String sala;
	String estado;
	
	public User(int t_port){
		this.port = t_port;
		this.sala = "undef";
		this.nome = "undef";
		this.estado = "init";
	}
	
	public void setName(String t_nome){
		this.nome = t_nome;
		this.estado = "outside";
	}
	
	public void setRoom(String t_sala){
		this.sala = t_sala;
		this.estado = "inside";
	}
	
	public void leaveRoom(){
		this.sala = "undef";
		this.estado = "outside";
	}
}

public class ChatServer {
	
	static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

	static private final Charset charset = Charset.forName("UTF8");
	static private final CharsetDecoder decoder = charset.newDecoder();
	static private Selector selector;
	static private User[] u_array = new User[99];
	static private String[] salas = new String[99];
	static private int nr_salas = 0;
	static private int i = 0;

	static public void main( String args[] ) throws Exception {
		
		int port = Integer.parseInt( args[0] );

		try {

			ServerSocketChannel ssc = ServerSocketChannel.open();

			ssc.configureBlocking( false );

			ServerSocket ss = ssc.socket();
			InetSocketAddress isa = new InetSocketAddress( port );
			ss.bind( isa );

			Selector selector2 = Selector.open();
			selector = selector2;

			ssc.register( selector, SelectionKey.OP_ACCEPT );
			System.out.println( "Listening on port "+port );

			while (true) {
				int num = selector.select();

				if (num == 0) {
					continue;
				}
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator();
				while (it.hasNext()) {

					SelectionKey key = it.next();

					if ((key.readyOps() & SelectionKey.OP_ACCEPT) ==
						SelectionKey.OP_ACCEPT) {
						Socket s = ss.accept();
						System.out.println( "Got connection from "+s );
						User u = new User(s.getPort());
						u_array[i]= u;
						i++;
						SocketChannel sc = s.getChannel();
						sc.configureBlocking( false );

						sc.register( selector, SelectionKey.OP_READ );
					} 
					else if ((key.readyOps() & SelectionKey.OP_READ) ==
						SelectionKey.OP_READ) {

						SocketChannel sc = null;

						try {
							sc = (SocketChannel)key.channel();
						
							boolean ok = processInput( sc );
							
							if (!ok) {
								key.cancel();

								Socket s = null;
								try {
									s = sc.socket();
									System.out.println( "Closing connection to "+s );
									s.close();
								} catch( IOException ie ) {
									System.err.println( "Error closing socket "+s+": "+ie );
								}
							}
						} catch( IOException ie ) {

							key.cancel();

							try {
							sc.close();
							} catch( IOException ie2 ) { 
								System.out.println( ie2 ); }

							System.out.println( "Closed "+sc );
						}
					}
				}

				keys.clear();
			}
		} catch( IOException ie ) {
			System.err.println( ie ); }
	}

	static private void printDef(int nr_user) throws IOException{
		for(int k=0; k<i; k++){
			sendOne(""+u_array[k].port, nr_user);
			sendOne(u_array[k].nome, nr_user);
			sendOne(u_array[k].sala, nr_user);
			sendOne(u_array[k].estado, nr_user);
			System.out.println(u_array[k].port);
			System.out.println(u_array[k].nome);
			System.out.println(u_array[k].sala);
			System.out.println(u_array[k].estado);
			System.out.println("");
		}
	}
	
	static private void printUsers(int nr_user) throws IOException{
		for(int k=0; k<i; k++){
			if(!u_array[k].estado.equals("init")){
				sendOne(u_array[k].nome, nr_user);
				System.out.println(u_array[k].nome);
			}
		}
	}
	
	static private void printSalas(int nr_user) throws IOException{
		for(int k=0; k<nr_salas; k++){
			sendOne(salas[k],nr_user);
			System.out.println(salas[k]);	
		}
	}
	
	static private boolean checkName(String name){
		for(int k=0; k<i; k++){
			if(u_array[k].nome.equals(name)){
				return false;
			}
		}
		return true;
	}
	
	static private int getNr(int port){
		for(int k=0; k<i; k++){
			if(u_array[k].port == port){
				return k;
			}
		}
		return -1;
	}
	
	static private int getNrN(String name){
		for(int k=0; k<i; k++){
			if(u_array[k].nome.equals(name)){
				return k;
			}
		}
		return -1;
	}
	
	static private void mkRoom(String room){
		int flag=0;
		for(int k=0; k<nr_salas; k++){
			if(salas[k].equals(room)) flag=1;
		}
		if (flag==0){
			salas[nr_salas]=room;
			nr_salas++;
		}
	}
	
	static private void command(String[] comm, int nr_user) throws IOException{
		
		switch (comm[0]) {
            case "nick":
				if(comm.length==2){
					String nome = comm[1];
					String nome_ant = u_array[nr_user].nome;
					String grup_t = u_array[nr_user].sala;
					if(checkName(nome)){
						u_array[nr_user].setName(nome);
						sendOne("OK",nr_user);
						if(!grup_t.equals("undef"))
							sendGroup("NEWNICK " + nome_ant + " " + nome +"\r\n", grup_t);
					}
					else{
						sendOne("ERROR",nr_user);
					}
				}else sendOne("ERROR",nr_user);
				break;
            case "join":
				if(u_array[nr_user].nome.equals("undef"))
					sendOne("ERROR",nr_user);
				else{
					if(!u_array[nr_user].sala.equals("undef")){
						String room_t = u_array[nr_user].sala;
						u_array[nr_user].leaveRoom();
						sendGroup("LEFT " + u_array[nr_user].nome +"\r\n", room_t);
					}
					if(comm.length==2){
						u_array[nr_user].setRoom(comm[1]);
						mkRoom(comm[1]);
						sendOne("OK",nr_user);
						sendGroup("JOINED " + u_array[nr_user].nome +"\r\n", comm[1]);
					}else sendOne("ERROR",nr_user);
				}
				break;
            case "leave":  
				if(u_array[nr_user].sala.equals("undef"))
					sendOne("ERROR",nr_user);
				else{
					String room_t = u_array[nr_user].sala;
					u_array[nr_user].leaveRoom();
					sendOne("OK",nr_user);
					sendGroup("LEFT " + u_array[nr_user].nome +"\r\n", room_t);
				}
				break;
            case "bye":
				String room_t2 = u_array[nr_user].sala;
				if(!u_array[nr_user].sala.equals("undef")){
					u_array[nr_user].leaveRoom();
					sendGroup("LEFT " + u_array[nr_user].nome +"\r\n", room_t2);
				}
				sendOne("BYE",nr_user);
				closeClient(nr_user);
                break;
            case "priv":  
				if(u_array[nr_user].nome.equals("undef"))
					sendOne("ERROR",nr_user);
				else{
					if(comm.length>=3){
						int nr_recep = getNrN(comm[1]);
						if(nr_recep!=-1){
							String msg_f = "";
							for(int ii=2; ii<comm.length; ii++){
								msg_f=msg_f + comm[ii]  + " ";
							}
							sendOne("PRIVATE " + u_array[nr_user].nome + " " + msg_f, nr_recep);
							sendOne("OK",nr_user);
						}
						else sendOne("ERROR",nr_user);
					}else sendOne("ERROR",nr_user);
				}
                break;
            case "print":
				if(comm.length > 1){
				switch (comm[1]) {
						case "users":
							printUsers(nr_user);
							break;
						case "rooms":
							printSalas(nr_user);
							break;
						default:
							printDef(nr_user);
							break;					
					}
				}else sendOne("ERROR",nr_user);
                break;
            default: 
				sendOne("ERROR",nr_user);
				System.out.println("Unknown command");
				break;
        }
	}
	
	
	static private boolean processInput( SocketChannel sc ) throws IOException {

		String message = " ";
		while (message.charAt(message.length()-1)!='\n') {
			buffer.clear();
			sc.read( buffer );
			buffer.flip();
			if (buffer.limit()!=0) {
				message = message + decoder.decode(buffer).toString();
			}
		}
		
		message = message.substring(1);
		
		Socket sckt = sc.socket();
		int lp = sckt.getPort();
		int nr = getNr(lp);
		
		if (message.charAt(0)=='/' && message.charAt(1)!='/'){
			String[] split = message.substring(1).split("\\s+");
			if(split[0].equals("priv")) command(split, nr);
			else if(split.length<=3) command(split, nr);
			else sendOne("ERROR",nr);
		}
		else if(message.charAt(0)=='/' && message.charAt(1)=='/'){
			message = message.substring(1);
			System.out.print("msg: " + message );
			
			if(u_array[nr].sala.equals("undef"))
				sendOne("ERROR", nr);
			else{
				message = "MESSAGE " + u_array[nr].nome + " " + message;				
				sendGroup(message, u_array[nr].sala);
			}
		}
		else{	
			System.out.print("msg: " + message );
			
			if(u_array[nr].sala.equals("undef"))
				sendOne("ERROR", nr);
			else{
				message = "MESSAGE " + u_array[nr].nome + " " + message;				
				sendGroup(message, u_array[nr].sala);
			}
		}
		return true;
	}
	
	static private void sendOne(String msg, int nr_user) throws IOException {
		
		msg = msg + "\r\n";
		
		ByteBuffer msgBuf=ByteBuffer.wrap(msg.getBytes());
		
		for(SelectionKey key : selector.keys()) {
			if(key.isValid() && key.channel() instanceof SocketChannel) {
				SocketChannel sch=(SocketChannel) key.channel();
				Socket temp_s = sch.socket();
				if(u_array[nr_user].port == temp_s.getPort()){
					while(msgBuf.hasRemaining()){
						
						 sch.write(msgBuf);
					 }
					msgBuf.rewind();
					break;
				}
			}
		}
	}
	
	static private void closeClient(int nr_user) throws IOException {
		
		for(SelectionKey key : selector.keys()) {
			if(key.isValid() && key.channel() instanceof SocketChannel) {
				SocketChannel sch=(SocketChannel) key.channel();
				Socket temp_s = sch.socket();
				if(u_array[nr_user].port == temp_s.getPort()){
					key.cancel();
					try {
						System.out.println( "Closing connection to "+temp_s);
						temp_s.close();
					} catch( IOException ie ) {
						System.err.println( "Error closing socket "+temp_s+": "+ie );
					}
					break;
				}
			}
		}
	}
	
	static private void sendGroup(String msg, String group) throws IOException {
		
		ByteBuffer msgBuf=ByteBuffer.wrap(msg.getBytes());
		
		for(SelectionKey key : selector.keys()) {
			if(key.isValid() && key.channel() instanceof SocketChannel) {
				SocketChannel sch=(SocketChannel) key.channel();
				Socket temp_s = sch.socket();
				int nr_user = getNr(temp_s.getPort());
				if(u_array[nr_user].sala.equals(group)){
					while(msgBuf.hasRemaining()){
						 sch.write(msgBuf);
					 }
					msgBuf.rewind();
				}
			}
		}
	}
	
	static private void sendAll(String msg) throws IOException {
		
		ByteBuffer msgBuf=ByteBuffer.wrap(msg.getBytes());
		
		for(SelectionKey key : selector.keys()) {
			if(key.isValid() && key.channel() instanceof SocketChannel) {
				SocketChannel sch=(SocketChannel) key.channel();
				while(msgBuf.hasRemaining()){
					
					 sch.write(msgBuf);
				 }
				msgBuf.rewind();
			}
		}
	}
}
