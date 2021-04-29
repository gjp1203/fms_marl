package supervisor_agents;
import java.io.*;
import java.util.ArrayList;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import communication_functions.Object_msg;
import communication_functions.Msg_serialization;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class Socket_in extends Agent {
	
	File SocketInTxt = new File("SocketIn.txt");
	File ListOfAgentsTxt = new File("ListOfAgents.txt");

	
	public static final int DEFAULT_PORT = 30009;
	
	private ServerSocket sSocket = null;
	private int port;
	
	public Socket_in() {
		this.port = DEFAULT_PORT;
		try {
			sSocket = new ServerSocket(this.port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Socket_in(int newPort) {
		this.port = newPort;
		try {
			sSocket = new ServerSocket(newPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setup() {
		try {
			if (SocketInTxt.exists()) {
				SocketInTxt.delete();
			}
			SocketInTxt.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			if (ListOfAgentsTxt.exists()) {
				ListOfAgentsTxt.delete();
			}
			ListOfAgentsTxt.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		addBehaviour(new Datenweiterleitung());
		
		System.out.println(getLocalName() + " initialisiert");
		
	}

	private class Datenweiterleitung extends CyclicBehaviour {
		public void action() {
			try {
				ArrayList<AID> Agentenregister;
				
				Socket cSocket = sSocket.accept();
				
				BufferedReader inBuff = new BufferedReader(new InputStreamReader(cSocket.getInputStream())); //Input-Stream in Puffer laden
				
				// Documentation of Messages in Txt File
				BufferedWriter writer = new BufferedWriter(new FileWriter(SocketInTxt, true));
				
				// -- End of Documentation --
				
				Object_msg newMsg = new Object_msg(inBuff); //Input als MsgObject speichern
				
				writer.append(newMsg.getDocumentation() + "\n");
				writer.flush();
				writer.close();
				
				cSocket.close();
				DFAgentDescription searchDfd = new DFAgentDescription();
		        ServiceDescription searchSd  = new ServiceDescription();
		        		        
		        searchDfd.addServices(searchSd);
		        
		        Agentenregister = Initialization_agent.Agentenregister;
		      
		        DFAgentDescription[] result = DFService.search(this.getAgent(), searchDfd);
		        //Msg_serialization ms = new Msg_serialization();
		        // Documentation in ListOfAgents File 
		        BufferedWriter writerAgents = new BufferedWriter(new FileWriter(ListOfAgentsTxt, true));
		        /*
		        for(int i = 0; i < Agentenregister.size(); i++) {
		        	writerAgents.append(Agentenregister.get(i).getLocalName() + "|");
		        	if (i+1 == Agentenregister.size()) {
		        		writerAgents.append("\n");
		        		writerAgents.flush();
		        		writerAgents.close();
		        	}
		        }*/
		        
		        if (result.length > 0) {
		        	ACLMessage aclm = new ACLMessage(ACLMessage.INFORM);
		        	aclm.setContent(newMsg.getContent()); //Inhalt mittels setcontent aus Object_msg festlegen
		        	
		        	int j = 0;
		        	//for(int i = 0; i < result.length; i++) {
		        	for(int i = 0; i < Agentenregister.size(); i++) {
		        		//if(result[i].getName().getLocalName().equals(newMsg.getSender())) {
		        		if(Agentenregister.get(i).getLocalName().equals(newMsg.getSender())) {
		        			/*for (int x=0; x< Agentenregister.size();x++) {
		        				System.out.println("AddReceiver - SocketIn: " + result[i].getName() + " Agentenregister: " + Agentenregister.get(x).getName());
		        				System.out.println(Agentenregister.get(x));
		        			}*/
		        			//aclm.addReceiver(result[i].getName());
		        			aclm.addReceiver(Agentenregister.get(i));
		        			System.out.println("Receiver: " + Agentenregister.get(i));
		        			j = j+1;//Empfänger finden	
		        		}
		        	}
		        	System.out.println("Socket in j: "+j +"for Sender: " + newMsg.getSender());
		        	
		        	if(j>0) {
		        	aclm.setConversationId(newMsg.getDatentyp());
		        	send(aclm);
		        	}
		        	if(j == 0) {
			        	ACLMessage msg1 = new ACLMessage(ACLMessage.REQUEST);
		        		msg1.addReceiver(new AID("Initialization_agent", AID.ISLOCALNAME));
		        		
		        		Msg_serialization ms = new Msg_serialization();
						   ms.setAgentenname(newMsg.getSender());
						   ms.setAgententyp(newMsg.getAgententyp());
						   ms.setDatentyp(newMsg.getDatentyp());
						   ms.setContent(newMsg.getContent());
		        		
						   try {
							      msg1.setContentObject(ms);
							   }
							   catch (Exception ex) { ex.printStackTrace(); }
		        		send(msg1);
			        }
		        }

				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FIPAException e) {
				e.printStackTrace();
			}
		}
	}
}