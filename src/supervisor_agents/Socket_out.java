package supervisor_agents;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.io.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;


public class Socket_out extends Agent {
	
	File SocketOutTxt = new File("SocketOut.txt");
	
	public static final int DEFAULT_PORT = 30008;
	public static final String DEFAULT_ADDRESS = "127.0.0.1";
	public static final String PLANT_OBJECT = "PlantObject";
		
	private int port;
	private String address;
		
	public Socket_out() {
		port = DEFAULT_PORT;
		address = DEFAULT_ADDRESS;
	}
		
	public Socket_out(String servAddr, int newPort) {
		port = newPort;
		address = servAddr;
	}
		
	public void setup() {
		try {
			if (SocketOutTxt.exists()) {
				SocketOutTxt.delete();
			}
			SocketOutTxt.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
 			DFAgentDescription dfd = new DFAgentDescription();
	  		dfd.setName(getAID());
	  		ServiceDescription sd = new ServiceDescription();
	  		sd.setName("Socket_out");
	  		sd.setType("Socket_out");
	  		dfd.addServices(sd);
	  		
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		addBehaviour(new Datenübermittlung());
		
		System.out.println(getLocalName() + " initialisiert");
	}
	
//--------------------------Cyclic Behaviour für die Übermittlung der Steuerungsentscheidung an Plant Simulation--------------------------------------------------
	
	private class Datenübermittlung extends CyclicBehaviour {
		
		public void action() {
			ACLMessage msg1 = receive();
			if (msg1 != null) {
				try {
					Socket socket = new Socket(address, port);
						
					PrintWriter outPrint = new PrintWriter(socket.getOutputStream(), true);
					
					// Documentation of Messages in Txt File
						BufferedWriter writer = new BufferedWriter(new FileWriter(SocketOutTxt, true));
						writer.append(msg1.getContent()+ "\n");
						writer.flush();
						writer.close();
					// -- End of Documentation --
					
					outPrint.println(msg1.getContent());
					System.out.println("Steuerungsentscheidung: " + msg1.getSender().getLocalName() + ": " + msg1.getContent());

					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			} else {
				block();
			}
		}
	}
}