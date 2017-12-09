import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
	String server;
	int port;
	Socket clientSocket;
    
    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    
    // Construtor
    public ChatClient(String server_tmp, int port_tmp) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                   chatBox.setText("");
                }
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

		server=server_tmp;
		port=port_tmp;

    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String sentence) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
			
		
		DataOutputStream outToServer =
			new DataOutputStream(clientSocket.getOutputStream());
		
		sentence = sentence+"\r\n";
		byte[] buf = sentence.getBytes("UTF-8");

		outToServer.write(buf, 0, buf.length);		
    }

    
    // Método principal do objecto
    public void run() throws IOException {
		String sentence;
		
		clientSocket = new Socket(server, port);
		BufferedReader inFromServer = new BufferedReader(new
			InputStreamReader(clientSocket.getInputStream()));
		while(true){
			sentence = inFromServer.readLine();
			System.out.println(sentence);
			
			String[] split = sentence.split("\\s+");
			String msg_f = "";
			
			switch(split[0]){
				case "MESSAGE":
					String msg_t = "";
					for(int ii=2; ii<split.length; ii++){
						msg_t=msg_t + split[ii]  + " ";
					}
					msg_f = msg_f + split[1] + ": " + msg_t;
					break;
				case "NEWNICK":
					msg_f = msg_f + split[1] + " mudou de nome para " + split[2];
					break;
				case "JOINED":
					msg_f = msg_f + split[1] + " entrou na sala";
					break;
				case "LEFT":
					msg_f = msg_f + split[1] + " saiu da sala";
					break;
				case "PRIVATE":
					msg_t = "";
					for(int ii=2; ii<split.length; ii++){
						msg_t=msg_t + split[ii]  + " ";
					}
					msg_f = msg_f + "PM de " + split[1] + ": " + msg_t;
					break;
				case "BYE":
					for(int ii=0; ii<split.length; ii++){
						msg_f=msg_f + split[ii]  + " ";
					}
					System.exit(0);
					break;
				default:
					for(int ii=0; ii<split.length; ii++){
						msg_f=msg_f + split[ii]  + " ";
					}
					break;
			}	
			chatArea.append(msg_f+"\n");
		}        
    }
    

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}
