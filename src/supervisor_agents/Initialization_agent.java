package supervisor_agents;
import jade.core.Agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;

import communication_functions.Msg_serialization;
import dqn_functions.Initialization_replay_memory;
import dqn_functions.Initialization_replay_memory_Maschine;
import dqn_functions.Transfer_replay_memory;
import jade.core.AID;
import jade.core.AgentContainer;
import jade.core.ContainerID;
import jade.core.behaviours.ActionExecutor;
import jade.core.behaviours.OutcomeManager;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.ams;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.CreateAgent;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

//--------------------------Initalisierung des Initialisierungsagentens------------------------------------------------------------------------------------------------

public class Initialization_agent extends Agent {
	private static final long serialVersionUID = 875534367892872L;
	
	public static boolean StartDqn = false; 
	public static boolean StartDqnMaschine = false; 
	public static int Umfang_BE_Gruppe  ;
	public static int Anzahl_Maschinen  ; // Anzahl Maschinen je Arbeitsgang
	private static int Wiederholung_Experiment = 0;
	private static int Experiment = 0;
	private static int Anzahl_steps;
	
	public static ArrayList<AID> Agentenregister = new ArrayList<AID>();
	private String[] content_init_array = new String [3];
	private String containerName;
	public static int Durchlauf = 2; // Ursprünglich 1
	private static ArrayList<AID> Receiver = new ArrayList<AID>();
	
	@Override
	public void setup() {
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Initialisierung_Agent");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		Initialization_replay_memory.createNewDatabase("Replay_Memory_global"); // Initialisierung des globalen Replay Memories
		//Initialization_replay_memory_Maschine.createNewDatabase("Replay_Memory_global"); 
		
		addBehaviour(new Agenteninitialisierung());
		
		System.out.println(getLocalName()+" initialisiert");
		
		// Get the name of the container where to create an agent as first argument (default: Main Container)
		containerName = AgentContainer.MAIN_CONTAINER_NAME;
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			containerName = (String) args[0];
		}
	}
		
		
		// Wait a bit and then create an agent of class jade.core.Agent in the indicated container
		//System.out.println("Hello. I'm going to create a new agent in container "+containerName+" in 5 seconds...");
		private class Agenteninitialisierung extends CyclicBehaviour {
			@Override
			public void action() {
				// Request the AMS to perform the CreateAgent action of the JADEManagementOntology
				// To do this use an ActionExecutor behaviour requesting the CreateAgent action and expecting no result (Void) 
				ACLMessage msg1 = receive();
				if(msg1 != null) {
				java.io.Serializable content = null; // Deserialisierung des Inhalts über Klasse Msg_serialization
				
				System.out.println("Msg1 Performative: " + msg1.getPerformative() + " Msg1.Sender: " + msg1.getSender().getLocalName() + " ,INFORM="+ ACLMessage.INFORM + "Sender: " + myAgent.getLocalName());
				System.out.println("Msg1.Sender:" + msg1.getSender().getLocalName().toString());
				Iterator iterator = Receiver.iterator();
				iterator = msg1.getAllReceiver();
				while (iterator.hasNext()) {
					//Receiver.add((AID) iterator.next());
					System.out.print("Iterator: " + ((AID) iterator.next()).getLocalName());
					System.out.print(" MyAgent: " + myAgent.getLocalName());
					//System.out.print("Receiver:" + Receiver.toString());
					
				}
				System.out.println();
				iterator = msg1.getAllReceiver();
				if (msg1.getPerformative() == (ACLMessage.INFORM) && !msg1.getSender().getLocalName().equals("ams") && ((AID)iterator.next()).getLocalName().equals(myAgent.getLocalName())){
						//msg1.getSender().getLocalName().equals(myAgent.getLocalName())) {
					
					System.out.println("Experiment: " + Experiment + " vs. Durchlauf: " + Durchlauf + " vs. WiederholungExperiment: " + Wiederholung_Experiment);
	
					String content_init = msg1.getContent();
					System.out.println("Content_init: "+ content_init);
					content_init_array = content_init.split("\\.");
					System.out.println("Content_init_split: "+ Arrays.toString(content_init_array));
					
						Umfang_BE_Gruppe = Integer.parseInt(content_init_array[0]);
						Anzahl_Maschinen = Integer.parseInt(content_init_array[1]);
						Wiederholung_Experiment = Integer.parseInt(content_init_array[2]);
					
					if (Integer.toString(Experiment).equals(Integer.toString(Wiederholung_Experiment))) { 
						Durchlauf++;
						Experiment = 0;
						}
					Experiment++;
					
					if (Durchlauf == 5) {Durchlauf = 1;} // Zurücksetzen auf 1
					StartDqn = false;
					Anzahl_steps = Transfer_replay_memory.resetAnzahl_steps();
					Transfer_replay_memory.resetMemory();
					
					System.out.println("Experiment: " + Experiment + " vs. Durchlauf: " + Durchlauf);
					System.out.println("Umfang_BE_Gruppen: " + Umfang_BE_Gruppe + " Anzahl_Maschinen :" + Anzahl_Maschinen + " Wiederholung_Experiment :" + Wiederholung_Experiment);
					System.out.println("Durchlauf: " + Durchlauf + ", Anzahl steps: " + Anzahl_steps);
				}
				
				if (msg1.getPerformative() == ACLMessage.REQUEST) {
				//System.out.println("Creating agent!");
				
				
				try {
					content = msg1.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				
				CreateAgent ca = new CreateAgent();
				String Name = ((Msg_serialization) content).getAgentenname();
				String Daten = ((Msg_serialization) content).getContent();
				String Typ = ((Msg_serialization) content).getAgententyp();
				
				ca.setAgentName(Name);
				
				
				if (Typ.equals("Auftrag")) {
					ca.setClassName("order_agents.Order_agent");
				}
				else {
					ca.setClassName("machine_agents." + Typ + "_agent");
				}
				ca.setContainer(new ContainerID(containerName, null));
				
				ACLMessage msg2 = new ACLMessage(ACLMessage.INFORM);
				AID NameInRegister = new AID(Name, AID.ISLOCALNAME);
				
				Agentenregister.add(Agentenregister.size(), NameInRegister);
				
				msg2.addReceiver(new AID(Name, AID.ISLOCALNAME));
				msg2.setConversationId(((Msg_serialization) content).getDatentyp());
				msg2.setContent(Daten);
				send(msg2);
				
				// Verlagert in Auftragsagenten
				/*
				if(Typ.equals("Auftrag")) {
					ACLMessage msg3 = new ACLMessage(ACLMessage.INFORM);
					msg3.addReceiver(new AID("Socket_out", AID.ISLOCALNAME));
					String Inhalt = "AuftragsAgent_initialisiert:" + ca.getAgentName();
					msg3.setContent(Inhalt);
					send(msg3);
				}*/
				
				ActionExecutor<CreateAgent, Void> ae = new ActionExecutor<CreateAgent, Void>(ca, JADEManagementOntology.getInstance(), getAMS()) {
					@Override
					public int onEnd() {
						int ret = super.onEnd();
						if (getExitCode() == OutcomeManager.OK) {
							// Creation successful
							//System.out.println("Agent successfully created");
							
							ACLMessage msg2 = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							msg2.addReceiver(new AID(Name, AID.ISLOCALNAME));
							msg2.setContent(Daten);
							send(msg2);
						}
						else {
							// Something went wrong
							//System.out.println("Agent creation error. "+getErrorMsg());
						}
						return ret;
					}
				};
				addBehaviour(ae);
				}
				} else {
					block();
				}
			}
		};
}