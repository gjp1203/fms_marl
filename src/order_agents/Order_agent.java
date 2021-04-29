package order_agents;
import jade.core.Agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.text.DecimalFormat;

import org.tensorflow.Graph;

import communication_functions.Msg_serialization;
import dqn_functions.Initialization_replay_memory;
import dqn_functions.Tensorflow;
import dqn_functions.Write_local_memory;
//import dqn_functions.Load_graph;
import dqn_functions.Transfer_replay_memory;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import supervisor_agents.Initialization_agent;

//--------------------------Initalisierung des Auftragsagentens------------------------------------------------------------------------------------------------

public class Order_agent extends Agent {

	private boolean Lager_vorgeschaltet = false;
	
	ArrayList<String> Arbeitsvorgangsfolge = new ArrayList<String>(); // Arbeitsgang 1:Arbeitsgang 2:...
	ArrayList<String> Auftragsdaten = new ArrayList<String>(); // Länge:Durchmesser:Tiefe:Achsenzahl:Material
	ArrayList<String> Auftragsgruppendaten = new ArrayList<String>(); //
	private AID[] Bearbeitungsstationen; // Liste aller Maschinenagenten eines Prozesses
	private AID[] Auftragssuche; // Liste aller Aufträge im MAS
	private String aktuelle_Station; // Speichern der aktuellen Station, auf der der Auftrag derzeit bearbeitet wird
	//private Graph g;
	private String state_t;
	private String state_t_;
	private int a_t;
	private double r_t;
	private String[] Aufträge_in_Gruppe = new String[1];
	private String Agententyp = "Auftrag";
	
	// Zu berücksichtigende Aufträge in Gruppenverwaltung a 3 Einträge + 5 (dieser Auftrag) 
	private int Anzahl_Maschinen = Initialization_agent.Anzahl_Maschinen;
	private int Umfang_BE_Gruppe = ((Initialization_agent.Umfang_BE_Gruppe-1)*3) + 2 + Anzahl_Maschinen; 
	
	public void setup() {
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(Agententyp); // Festlegung des Agententyps Auftrag
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		for (int i = 0; i < 5; i++) { // Erstellung der notwendigen Felder in der ArrayList für die Auftragsdaten
			Auftragsdaten.add(i, null);
		}
		
		/* Verlagert in Tensorflow Methode 
		if (Transfer_replay_memory.getAnzahl_steps() >= Tensorflow.get_warm_up_steps()) {
			try {
				g = Load_graph.graph();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}*/
		
		Initialization_replay_memory.createNewDatabase(getLocalName()); // Initialisierung des lokalen Replay Memories
		//Initialization_replay_memory_Maschine.createNewDatabase(getLocalName()); // Verfälscht sonst Datensätze
		
		addBehaviour(new Stammdatenverwaltung()); // Initialisierung des Cyclic Behaviours für das Aufrufen der Steuerungsfunktionen
		addBehaviour(new Bewegungsdatenverwaltung()); // Initialisierung des Cyclic Behaviours für das Aufrufen der Steuerungsfunktionen
		addBehaviour(new Auftragsgruppenverwaltung()); // Initialisierung des Cyclic Behaviours für das Aufrufen der Steuerungsfunktionen
		addBehaviour(new Auftragsabschluss()); // Initialisierung des Cyclic Behaviours für das Aufrufen der Steuerungsfunktionen
		
		System.out.println(getLocalName() + " initialisiert");
		
		ACLMessage msg3 = new ACLMessage(ACLMessage.INFORM);
		msg3.addReceiver(new AID("Socket_out", AID.ISLOCALNAME));
		String Inhalt = "AuftragsAgent_initialisiert: " + this.getLocalName();
		msg3.setContent(Inhalt);
		send(msg3);
	}
	
	protected void takeDown() {
		// Deregister from the yellow pages
		
		try {
			DFService.deregister(this);
			
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	private class Stammdatenverwaltung extends CyclicBehaviour {
		
		private MessageTemplate Stammdaten; // Wird durch Ende_Zeit in Plant Simulation genutzt
		
		public void action() {
			
			Stammdaten = MessageTemplate.MatchConversationId("S"); // Nachrichten über den Abschluss des Auftrags auswählen
			
			ACLMessage Stammdaten_Nachricht = myAgent.receive(Stammdaten);
			
			if (Stammdaten_Nachricht != null) { // Prüfen, ob Nachricht über den Abschluss des Auftrags erhalten wurde
				System.out.println ("Stammdaten - Order Agent " + this.getAgent().getLocalName());
				
				String Nachricht_Inhalt = Stammdaten_Nachricht.getContent();
				String Zwischenspeicher = "";
				char aChar;
				int Datenfeld = 0;

				for (int i = 0; i < Nachricht_Inhalt.length(); i++) { // Jedes Zeichen der Nachricht wird einzeln geprüft
			
					aChar = Nachricht_Inhalt.charAt(i);
			
					if (aChar == ';') { // Prüfen, ob Ende der Nachricht erreicht
						return;
					}
					else if (aChar == ':') { //Prüfen, ob Ende des jeweiligen Datums erreicht
						Datenfeld++; // Wenn ja, dann neues Datenfeld markieren
						Zwischenspeicher = ""; // und Zwischenspeicher für dieses Datenfeld zurücksetzen
					}
					else if (aChar != ';' && aChar != ':') { // Prüfen, ob aktuell Auftragsdaten gelesen werden (erste fünf Datenfelder)
						Zwischenspeicher = Zwischenspeicher + aChar; // Wenn ja, dann aktuelles Zeichen in Zwischenspeicher schreiben
						if (Auftragsdaten.size() < Datenfeld + 1) { // Prüfen, ob bereits mit dem Schreiben des Auftragsdatums in das jeweilige Feld begonnen wurde (+6, da erste 5 Daten durch Verfügbarkeitsprüfung geschrieben werden)
							Auftragsdaten.add(Zwischenspeicher); // Wenn nicht, dann Feld anlegen und mit Schreiben beginnen
						}
						else if (Auftragsdaten.size() >= Datenfeld + 1) { // Schreiben des Auftragsdatums in das jeweilige Feld wurde bereits begonnen
							Auftragsdaten.set(Datenfeld, Zwischenspeicher);// Schreiben fortsetzen
						}
					}
				}
				System.out.println(Auftragsdaten);
			} else {
				block();
			}
		}
	}
	
//--------------------------Cyclic Behaviour für die Belegungsplanung------------------------------------------------------------------------------------------	
	
	private class Bewegungsdatenverwaltung extends CyclicBehaviour {
		
		private MessageTemplate Belegungsplanung_Daten; // Wird durch Datenerfassung in Plant Simulation genutzt
		private MessageTemplate Rückmeldung_Belegungsplanung; // Wird für Verarbeitung der Rückmeldungen zur Belegungsplanung durch die Maschinenagenten genutzt
		private int step = 0;
		private int Anzahl_Rückmeldungen = 0; // Variable für das Zählen der von Maschinenagenten erhaltenen Rückmeldungen zur Belegungsplanung
		private AID bestStation = null; // Variable, um die aktuell am besten bewertetste Station zu speichern, null, um zu gewährleisten, dass 
		//private float bestBewertung; // Variable, um Bewertung der aktuell am besten bewertetste Station zu speichern
		private float Bearbeitungszeit; // Variable für die Berechnung und Weiterleitung der Bearbeitungszeit des Auftrags auf der ausgewählten Station
		//float[][] state = new float[1][5]; // Zustandsvektor für das Deep q-Learning
		private float[][] state = new float[1][Umfang_BE_Gruppe]; // Zustandsvektor für das Deep q-Learning
		private String[] action = new String[Anzahl_Maschinen]; // Zwischenspeicher für die Interpretation des Aktionswertes a_t
		
		public void action () {
			
			Belegungsplanung_Daten = MessageTemplate.and( // Definition von Nachrichten, die in diesem Behaviour (1. Schritt der Belegungsplanung) bearbeitet werden sollen
				MessageTemplate.MatchConversationId("B"), // Nachrichten mit Auftragsdaten für Belegungsplanung aus Plant Simulation
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			
			ACLMessage Belegung_Daten = myAgent.receive(Belegungsplanung_Daten); // Nur Nachrichten für den 1. Schritt der Belegungsplanung (Verarbeitung der Daten) empfangen
				
			if(Belegung_Daten != null) {

				String Nachricht_Inhalt = Belegung_Daten.getContent(); // Variablen für das Auslesen der Nachricht
				String Zwischenspeicher = "";
				char aChar;
				int Datenfeld = 0;
						
				for (int i = 0; i < Nachricht_Inhalt.length(); i++) { // Jedes Zeichen der Nachricht wird einzeln geprüft
								
					aChar = Nachricht_Inhalt.charAt(i);
								
					if (aChar == ';') { // Prüfen, ob Ende der Nachricht erreicht
						return;
					}
					else if (aChar == ':') { //Prüfen, ob Ende des jeweiligen Datums erreicht
						Datenfeld++; // Wenn ja, dann neues Datenfeld markieren
						Zwischenspeicher = ""; // und Zwischenspeicher für dieses Datenfeld zurücksetzen
					}
					else if (aChar != ';' && aChar != ':' && aChar != '|') { // Prüfen, ob aktuell Daten gelesen werden
						Zwischenspeicher = Zwischenspeicher + aChar; // Wenn ja, dann aktuelles Zeichen in Zwischenspeicher schreiben
						if (Auftragsdaten.size() < Datenfeld+6) { // Prüfen, ob bereits mit dem Schreiben des Auftragsdatums in das jeweilige Feld begonnen wurde (+6, da erste 5 Daten durch Verfügbarkeitsprüfung geschrieben werden)
							Auftragsdaten.add(Zwischenspeicher); // Wenn nicht, dann Feld anlegen und mit Schreiben beginnen
						}
						else if (Auftragsdaten.size() >= Datenfeld+6) { // Schreiben des Auftragsdatums in das jeweilige Feld wurde bereits begonnen
							Auftragsdaten.set(Datenfeld+5, Zwischenspeicher);// Schreiben fortsetzen
						}
					}
					else if (aChar == '|') {
						for (int j = i+1; j < Nachricht_Inhalt.length(); j++) {
							aChar = Nachricht_Inhalt.charAt(j);
							Zwischenspeicher = Zwischenspeicher + aChar;
						}
						if (!Zwischenspeicher.equals("")) {
							Aufträge_in_Gruppe = (Zwischenspeicher).split(",");
							System.out.println("Aufträge in Gruppe: " + Arrays.toString(Aufträge_in_Gruppe));
						}
						i = Nachricht_Inhalt.length();
					}
				}

				addBehaviour(new Belegungsplanung());
			} else {
				block();
			}
		}
	}

	
	
	private class Auftragsabschluss extends CyclicBehaviour {
		
		private MessageTemplate Bearbeitung_beendet; // Wird durch Ende_Zeit in Plant Simulation genutzt
		private String state_t_last;
		
		public void action() {
			ArrayList<AID> Agentenregister;
			Agentenregister = Initialization_agent.Agentenregister;
			
			Bearbeitung_beendet = MessageTemplate.MatchConversationId("L"); // Nachrichten über den Abschluss des Auftrags auswählen
			
			ACLMessage Abschluss_Rückmeldung = myAgent.receive(Bearbeitung_beendet);
			
			if (Abschluss_Rückmeldung != null) { // Prüfen, ob Nachricht über den Abschluss des Auftrags erhalten wurde
				if (Abschluss_Rückmeldung.getContent() != null) {
					r_t = Double.parseDouble(Abschluss_Rückmeldung.getContent());
					state_t_last = "0.0";
					for (int i=1; i<Anzahl_Maschinen; i++) { // Ausnullen aller Maschineninformationen und setzen 100% Fortschritt
						state_t_last = state_t_last + ",0.0";
						if (i == (Anzahl_Maschinen-1)) {
							state_t_last = state_t_last + ",0.0,1.0";
							for (int a = Anzahl_Maschinen+2; a<Umfang_BE_Gruppe; a=a+3) {
								state_t_last = state_t_last + ",0.0,0.0,0.0";
							}
						}
					}
				
					Write_local_memory app_1 = new Write_local_memory();
					//app_1.insert(getLocalName(), state_t, "0.0,0.0,0.0,0.0,1,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0", a_t, r_t, 0);
					app_1.insert(getLocalName(), state_t, state_t_last, a_t, r_t, 0);
					Transfer_replay_memory app_2 = new Transfer_replay_memory();
					app_2.Übertragung(getLocalName());
			
					System.out.println(getLocalName() + " wird gelöscht");
					
					Agentenregister.remove(myAgent.getAID());
					
					// Löschung mitteilen 
					ACLMessage Delete_Agent = new ACLMessage(ACLMessage.INFORM); // Wenn ja, dann Nachricht für die Rückmeldung des Ergebnisses der Belegungsplanung erstellen
					Delete_Agent.addReceiver(new AID("Socket_out", AID.ISLOCALNAME)); // Nachricht über Socket an Plant Simulation schicken
					Delete_Agent.setContent("D:" + getLocalName() +";");
					send(Delete_Agent); // Versenden der Rückmeldung
	
					myAgent.doDelete(); // Auftragsagent löschen
					//g = null;
					System.gc();
					
					
					/*
					for (int i = 0; i < Agentenregister.size(); i++) {
						if (Agentenregister.get(i).equals(myAgent.getAID())) {
							System.out.println("Fuck yeah - deleted!: " +myAgent.getLocalName()); //+ Agentenregister.get(i).getLocalName());
							Agentenregister.remove(i);
							//Agentenregister.remove(new AID(myAgent.getLocalName(), AID.ISLOCALNAME));
						}
					}*/
					}
				else {myAgent.doDelete();}
				//else {block();}
			} else {
				block();
			}
		}
	}
	
	private class Auftragsgruppenverwaltung extends CyclicBehaviour {
		
		private MessageTemplate Auftragsgruppenverwaltung; // Wird durch Datenerfassung in Plant Simulation genutzt
		private MessageTemplate Mitteilung_Auftragsgruppen;
		private String[] Auftragsdaten_Gruppe = new String[3];
		
		public void action() {
			
			Auftragsgruppenverwaltung = MessageTemplate.MatchConversationId("G");
			ACLMessage Gruppendaten = myAgent.receive(Auftragsgruppenverwaltung);
			
			
			if (Gruppendaten != null) {
				
				System.out.println("Name: "+ this.getAgent().getLocalName()+" Performative: " + Gruppendaten.getPerformative() + " Content: " + Gruppendaten.getContent());
			
				if (Gruppendaten.getPerformative() == ACLMessage.INFORM) {
					Auftragsdaten_Gruppe = (Gruppendaten.getContent()).split(":");
					System.out.println("Name: " + this.getAgent().getLocalName());
					System.out.println("Gruppendaten Content: " + Arrays.toString(Auftragsdaten_Gruppe));
					
				}
				
				else if (Gruppendaten.getPerformative() == ACLMessage.REQUEST) {
					System.out.println("Gruppendaten - ACL Request");
					ACLMessage Rückmeldung = Gruppendaten.createReply(); // Erstellen der Antwort auf die Datenanfrage für die Belegungsplanung des Auftrags
					Rückmeldung.setPerformative(ACLMessage.PROPOSE); // Maschinenagent meldet Daten zur möglichen Bearbeitung zurück
					Rückmeldung.setConversationId("GB");
					
					Msg_serialization ms = new Msg_serialization(); // Serialisierung der Bewegungsdaten über Klasse Msg_serialization
				
					   ms.setAuftragstyp(Auftragsdaten_Gruppe[1]);
					   ms.setSchlupfzeit(Float.parseFloat(Auftragsdaten_Gruppe[2]));
					   ms.setBearbeitungszeit_bis_aktuell(Float.parseFloat(Auftragsdaten_Gruppe[3]));
					   ms.setAgententyp(Auftragsdaten_Gruppe[0]);
					   
					   System.out.println(this.getAgent().getLocalName() +  " Auftragstyp: " + ms.getAuftragstyp() + " Schlupfzeit: " + ms.getSchlupfzeit()+ " Bearbeitungszeit_bis_aktuell: " + ms.getBearbeitungszeit_bis_aktuell());
					   
					try {
							Rückmeldung.setContentObject(ms); // Serialisierte Daten als Inhalt der Rückmeldung festlegen
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					send(Rückmeldung); // Versenden der Rückmeldung
					
					
				}
			} else {
				block();
			}
		}
	}
	private class Belegungsplanung extends Behaviour {
		
		private MessageTemplate Belegungsplanung_Daten; // Wird durch Datenerfassung in Plant Simulation genutzt
		private MessageTemplate Rückmeldung_Belegungsplanung; // Wird für Verarbeitung der Rückmeldungen zur Belegungsplanung durch die Maschinenagenten genutzt
		private MessageTemplate Rückmeldung_Auftragsgruppe; // Wird für Verarbeitung der Rückmeldungen zur Belegungsplanung durch die Maschinenagenten genutzt
		private int step = 0;
		private int Anzahl_Rückmeldungen = 0; // Variable für das Zählen der von Maschinenagenten erhaltenen Rückmeldungen zur Belegungsplanung
		private AID bestStation = null; // Variable, um die aktuell am besten bewertetste Station zu speichern, null, um zu gewährleisten, dass 
		//private float bestBewertung; // Variable, um Bewertung der aktuell am besten bewertetste Station zu speichern
		private float Bearbeitungszeit; // Variable für die Berechnung und Weiterleitung der Bearbeitungszeit des Auftrags auf der ausgewählten Station
		// float[][] state = new float[1][5]; // Zustandsvektor für das Deep q-Learning
		private float[][] state = new float[1][Umfang_BE_Gruppe]; // Zustandsvektor für das Deep q-Learning
		//String[] action = new String[3]; // Zwischenspeicher für die Interpretation des Aktionswertes a_t
		String[] action = new String[Anzahl_Maschinen]; // Zwischenspeicher für die Interpretation des Aktionswertes a_t
		private float min_Bearbeitungszeit = 0;
		private float max_Bearbeitungszeit = 0;
		private double reward;
		private double localreward;
		private double localreward_1;
		float[][] Bearbeitungszeiten = new float[1][Anzahl_Maschinen];
		private int Anzahl_Aufträge = 0;
		private int min_i;
		private int max_i;
		private int numOfRückmeldung; 
		private int randomInt;
		float[][] Maschinenzeitfaktor = new float[1][Anzahl_Maschinen];
		float[][] GesamtzeitPuffer = new float[1] [Anzahl_Maschinen];
		float[][] GesamtzeitPuffer_1 = new float[1] [Anzahl_Maschinen];
		float[][] KapazitaetMaschine = new float [1] [Anzahl_Maschinen];
		float[][] Zwischenspeicher_Zuordnung = new float[Anzahl_Maschinen][2]; 
				
		public void action () {

			switch (step) {
			case 0:
					
					if (Aufträge_in_Gruppe[0] != null) {
					
						ACLMessage Anfrage_Gruppe = new ACLMessage(ACLMessage.REQUEST);
						Anfrage_Gruppe.setConversationId("G");
						Anfrage_Gruppe.setContent("REQUEST an Anfrage_Gruppe");
						
						Anfrage_Gruppe.setReplyWith("Anfrage_Gruppe"); // Kennzeichnung der Antwort auf diese Anfrage definieren, um spätere Zuordnung über Templates zu ermöglichen
						
						System.out.println("Anfrage_Gruppe");
						
						for (int i = 0; i < Aufträge_in_Gruppe.length; i++) {
							
							if (Aufträge_in_Gruppe[i] != null) {
								Anzahl_Aufträge++;
								System.out.println(Aufträge_in_Gruppe[i]);
								
								DFAgentDescription template = new DFAgentDescription();
								ServiceDescription sd = new ServiceDescription();
								sd.setType(Agententyp); // Suche nach Agententyp "Auftrag"
								template.addServices(sd);
								
								try {
									DFAgentDescription[] result = DFService.search(myAgent, template); 
									Auftragssuche = new AID[result.length]; // Notwendige Länge der Liste aller passenden Maschinenagenten definieren
									for (int a = 0; a < result.length; a++) { // Agenten-ID jedes passenden Maschinenagentens in Liste schreiben
										Auftragssuche[a] = result[a].getName();
										
										if (Auftragssuche[a].getLocalName().equals(Aufträge_in_Gruppe[i].replace(":", "."))) {
											System.out.println( Auftragssuche[a] + "als Receiver hinzugefügt");
											Anfrage_Gruppe.addReceiver(Auftragssuche[a]);
										}
									}
								}
								
								catch (FIPAException fe) {
									fe.printStackTrace();
								}
							}
						}
						
						send(Anfrage_Gruppe); // Versenden der Anfrage
						
						for(Iterator iterator = Anfrage_Gruppe.getAllReceiver();
								iterator.hasNext();){
								            AID r = (AID) iterator.next();
								            System.out.println("Anfrage gesendet an: " + r.getLocalName() + " Performative Anfrage Gruppe: " + Anfrage_Gruppe.getPerformative());
						}
						
						Rückmeldung_Auftragsgruppe = MessageTemplate.and( // Template für Antworten auf Anfragen zur Belegungsplanung
								MessageTemplate.MatchConversationId("GB"), // die im nächsten Schritt (Verarbeitung der Bewegungsdaten der Maschinenagenten des folgenden Prozesschritts) bearbeitet werden sollen
								MessageTemplate.MatchInReplyTo(Anfrage_Gruppe.getReplyWith()));
						
						step++;
					}
					
					else {
						step = step + 2;
					}
				break;
			case 1:
					System.out.println("Belegungsplanung Step 1 von" + this.getAgent().getLocalName());
					ACLMessage Rückmeldung_Gruppe = myAgent.receive(Rückmeldung_Auftragsgruppe); // Empfangen aller Daten, die dem Template für Rückmeldungen entsprechen
				
					if (Rückmeldung_Gruppe != null) {
						Anzahl_Rückmeldungen++;
						System.out.println("Anzahl Rückmeldungen: " + Anzahl_Rückmeldungen + " Anzahl Aufträge: " + Anzahl_Aufträge);
						//System.out.println("Anzahl_Rückmeldungen:" + Anzahl_Rückmeldungen + " Content der Nachricht: " + Rückmeldung_Gruppe.getContent());
						java.io.Serializable Inhalt = null; // Deserialisierung des Inhalts über Klasse Msg_serialization
						try {
							Inhalt = Rückmeldung_Gruppe.getContentObject();
						}
						catch (UnreadableException e) {
							e.printStackTrace();
						}
						System.out.println(((Msg_serialization) Inhalt).getAuftragstyp()+" vs. "+ Auftragsdaten.get(5));
						System.out.println(((Msg_serialization) Inhalt).getAgententyp()+" vs. "+ Auftragsdaten.get(5));
						System.out.println(((Msg_serialization) Inhalt).getArbeitsgang()+" vs. "+ Auftragsdaten.get(5));
						
						if (((Msg_serialization) Inhalt).getAgententyp().equals(Auftragsdaten.get(5))) { // Auftragsdaten.get(5) := Auftrag_Typ_X
							state[0][5 + 3*(Anzahl_Rückmeldungen-1)] = 1;
						}
						else {
							state[0][5 + 3*(Anzahl_Rückmeldungen-1)] = 0;
						}
						state[0][5 + 3*(Anzahl_Rückmeldungen-1)+1] = ((Msg_serialization) Inhalt).getSchlupfzeit()/1500; // Bearbeitungszeit für den aktuellen Auftrag auf der jeweiligen Maschine berechnen
						state[0][5 + 3*(Anzahl_Rückmeldungen-1)+2] = ((Msg_serialization) Inhalt).getBearbeitungszeit_bis_aktuell()/1500; // Bearbeitungszeit für den aktuellen Auftrag auf der jeweiligen Maschine berechnen
						
						System.out.println("State in Step 1 Belegungsplanung: " + Arrays.deepToString(state));
						
						if (Anzahl_Rückmeldungen == Anzahl_Aufträge) {
							
							/*if (Anzahl_Aufträge < 2) {
								
								for (int i = 0; i < 2 - Anzahl_Aufträge; i++) {
									state[0][5 + 3*(Anzahl_Rückmeldungen+i-1)] = 0;
									state[0][5 + 3*(Anzahl_Rückmeldungen+i-1)+1] = 0;
									state[0][5 + 3*(Anzahl_Rückmeldungen+i-1)+2] = 0;
								}
							}*/
							Anzahl_Rückmeldungen = 0;
							for (int i = 0; i < Aufträge_in_Gruppe.length; i++) {
								Aufträge_in_Gruppe[i] = null;
							}
							step++;
						}
					} else {
						block();
					}
				break;
				
			case 2:
					
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					SearchConstraints searchConst = new SearchConstraints();
					sd.setType(Auftragsdaten.get(6)); // Arbeitsgang als Suchkriterium in allen Maschinenagenten festlegen
					template.addServices(sd);
					searchConst.setMaxResults((long)200);
				      //DFService.searchUntilFound(this.getAgent(), (new AID(newMsg.getSender(), AID.ISLOCALNAME)), searchDfd, searchConst , (long) 4000);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						Bearbeitungsstationen = new AID[result.length]; // Notwendige Länge der Liste aller passenden Maschinenagenten definieren
						for (int i = 0; i < result.length; i++) { // Agenten-ID jedes passenden Maschinenagentens in Liste schreiben
							Bearbeitungsstationen[i] = result[i].getName();
						}
					}
					
					catch (FIPAException fe) {
						fe.printStackTrace();
						step++;
						break;
					}
				
					ACLMessage Anfrage = new ACLMessage(ACLMessage.INFORM); // Anfrage an passende Maschinenagenten für die aktuellen Bewegungsdaten stellen
					Anfrage.setConversationId("B"); // ConversationId (B) für spätere Zuordnung über Templates definieren
					Anfrage.setReplyWith("Anfrage"); // Kennzeichnung der Antwort auf diese Anfrage definieren, um spätere Zuordnung über Templates zu ermöglichen
					for (int i = 0; i < Bearbeitungsstationen.length; i++) { // Alle Maschinenagenten des jeweiligen Prozesses als Empfänger hinzufügen
						Anfrage.addReceiver(Bearbeitungsstationen[i]);
					}
					
					Msg_serialization ms = new Msg_serialization(); // Serialisierung der Auftragsdaten über Klasse Msg_serialization
					   ms.setLänge(Integer.parseInt(Auftragsdaten.get(0)));
					   ms.setDurchmesser(Integer.parseInt(Auftragsdaten.get(1)));
					   ms.setTiefe(Integer.parseInt(Auftragsdaten.get(2)));
					   ms.setAchsenzahl(Integer.parseInt(Auftragsdaten.get(3)));
					   ms.setMaterial(Auftragsdaten.get(4));
					   
					try {
						Anfrage.setContentObject(ms); // Serialisierte Daten als Inhalt der Anfrage festlegen
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}

					send(Anfrage); // Versenden der Anfrage

					Rückmeldung_Belegungsplanung = MessageTemplate.and( // Template für Antworten auf Anfragen zur Belegungsplanung
							MessageTemplate.MatchConversationId("B"), // die im nächsten Schritt (Verarbeitung der Bewegungsdaten der Maschinenagenten des folgenden Prozesschritts) bearbeitet werden sollen
							MessageTemplate.MatchInReplyTo(Anfrage.getReplyWith()));
					
					step++;
				break;
			case 3:
				
				ACLMessage Rückmeldung = myAgent.receive(Rückmeldung_Belegungsplanung); // Empfangen aller Daten, die dem Template für Rückmeldungen entsprechen
				
				if (Rückmeldung != null) {
					Anzahl_Rückmeldungen++; // Anzahl der Rückmeldungen zählen
					
					if (Rückmeldung.getPerformative() == ACLMessage.PROPOSE) {
						
						java.io.Serializable Inhalt = null; // Deserialisierung des Inhalts über Klasse Msg_serialization
						try {
							Inhalt = Rückmeldung.getContentObject();
						}
						catch (UnreadableException e) {
							e.printStackTrace();
						}
						
						/*state[0][Anzahl_Rückmeldungen-1] = Integer.parseInt(Auftragsdaten.get(7))*((Msg_serialization) Inhalt).getBearbeitungszeitfaktor(); // Bearbeitungszeit für den aktuellen Auftrag auf der jeweiligen Maschine berechnen
						action[Anzahl_Rückmeldungen-1] = Rückmeldung.getSender().getLocalName();*/
						
						Float Bewertung = (((Msg_serialization) Inhalt).getZeitbisBearbeitung() // Bewertung der jeweiligen Maschine über vrsl. Zeit bis Fertigstellung des Prozesses
								+Integer.parseInt(Auftragsdaten.get(7))*((Msg_serialization) Inhalt).getBearbeitungszeitfaktor())
								/(((Msg_serialization) Inhalt).getVerfügbarkeit())*100;
						if (((Msg_serialization) Inhalt).getStatus() == "gestört") { // Prüfen, ob Maschine gestört
							Bewertung = Bewertung + ((Msg_serialization) Inhalt).getMTTR(); // Wenn ja, dann MTTR hinzurechnen
						}
						System.out.println("Anzahl_Rückmeldungen: " + Anzahl_Rückmeldungen);
						state[0][Anzahl_Rückmeldungen-1] = Bewertung; // Bearbeitungszeit für den aktuellen Auftrag auf der jeweiligen Maschine berechnen
						action[Anzahl_Rückmeldungen-1] = Rückmeldung.getSender().getLocalName();
						Zwischenspeicher_Zuordnung[Anzahl_Rückmeldungen-1][0]= Bewertung;
						Zwischenspeicher_Zuordnung[Anzahl_Rückmeldungen-1][1]= Anzahl_Rückmeldungen-1;
						
						Bearbeitungszeiten[0][Anzahl_Rückmeldungen-1] = Integer.parseInt(Auftragsdaten.get(7))*((Msg_serialization) Inhalt).getBearbeitungszeitfaktor();
						Maschinenzeitfaktor[0][Anzahl_Rückmeldungen-1] = ((Msg_serialization) Inhalt).getBearbeitungszeitfaktor();
						GesamtzeitPuffer[0][Anzahl_Rückmeldungen-1] = ((Msg_serialization) Inhalt).getGesamtzeitPuffer();
						GesamtzeitPuffer_1[0][Anzahl_Rückmeldungen-1] = ((Msg_serialization) Inhalt).getGesamtzeitPuffer_1();
						KapazitaetMaschine[0][Anzahl_Rückmeldungen-1] = ((Msg_serialization) Inhalt).getKapazitaetMaschine();
					}
					
					if (Anzahl_Rückmeldungen >= Bearbeitungsstationen.length) { // Prüfen, ob zu jeder Anfrage auch eine Rückmeldung erhalten wurde
												
						// Codierung der Maschinen hinsichtlich Processing Speed 
						Arrays.sort(Zwischenspeicher_Zuordnung, (a,b) -> Float.compare(a[0], b[0]));
						
						for (int i=0; i< Anzahl_Rückmeldungen; i++) {
							float position = Zwischenspeicher_Zuordnung[i][1];
							state[0][(int) position] = ((float)i)+1;
							state[0][(int)position] = state[0][(int)position] / 10;
							System.out.println(Arrays.deepToString(state));
						}
						
						System.out.println("Statevektor after Coding: " + Arrays.deepToString(state));
						/*
						min_i = 0;
						min_Bearbeitungszeit = state[0][0];
						for (int i = 0; i < Anzahl_Rückmeldungen; i++) {
							if (state[0][i] < min_Bearbeitungszeit) {
								min_Bearbeitungszeit = state[0][i]; 
								min_i = i; 
							}
						}
						
						max_i = 0;
						max_Bearbeitungszeit = state[0][0];
						for (int i = 0; i < Anzahl_Rückmeldungen; i++) {
							if (state[0][i] > max_Bearbeitungszeit) {
								max_Bearbeitungszeit = state[0][i]; 
								max_i = i; 
							}
						}


						// Zuweisung der Codierung "-1" , "0" ,"1" 
						if (max_i == min_i) {
							state[0][min_i] = -1;
							
						while (true) {
							try {
								Random rand = new Random();
								int r = rand.nextInt((Anzahl_Rückmeldungen - 1)+1);
								//System.out.println ("Random Int:" + r);
								if (r != min_i) {
									state[0][r]=1;
									break;
								}
							} catch (Exception e) {continue;}
							}
						} else {
						state[0][max_i] = 1;
						state[0][min_i] = -1;
						}
						
						for (int i = 0; i < Anzahl_Rückmeldungen; i++) {
							if (state[0][i] != 1 && state[0][i] != -1) {
								state[0][i] = 0;
							}
						}
						*/
						// Zuweisung der normalisierten Schlupfzeit und Auftragsfortschritt: 
						
						state[0][Anzahl_Rückmeldungen] = (Float.parseFloat(Auftragsdaten.get(8))/1500);
						state[0][Anzahl_Rückmeldungen+1] = Float.parseFloat(Auftragsdaten.get(9));
						System.out.println("Zustandsvektor_Order: " + Arrays.deepToString(state));
						
						System.out.println("Vorher:"+ Arrays.deepToString(action));
						
						int action_Wert = Tensorflow.action(state, Anzahl_Rückmeldungen);
						
						System.out.println("Nachher:"+ Arrays.deepToString(action));
						System.out.println("Durchlauf Init-Agent:"+ Initialization_agent.Durchlauf);
						System.out.println("Anzahl Rückmeldungen:"+Anzahl_Rückmeldungen + "Anzahl Aufträge: " + Anzahl_Aufträge);
						System.out.println("Action Wert:" + action_Wert + "Local Name:"+ AID.ISLOCALNAME);
						
						
							try {
								bestStation = new AID(action[action_Wert], AID.ISLOCALNAME);
							}
							catch (Exception e) {
								bestStation = new AID(action[0], AID.ISLOCALNAME);
								System.out.println(e);
							}
						
						Bearbeitungszeit = Bearbeitungszeiten[0][action_Wert];
						
						max_Bearbeitungszeit = state[0][0];
						for (int i = 0; i < Anzahl_Rückmeldungen; i++) {
							if (state[0][i] > max_Bearbeitungszeit) {
								max_Bearbeitungszeit = state[0][i]; 
							}
						}

						min_Bearbeitungszeit = state[0][0];
						for (int i = 0; i < Anzahl_Rückmeldungen; i++) {
							if (state[0][i] < min_Bearbeitungszeit) {
								min_Bearbeitungszeit = state[0][i]; 
							}
						}
						/*
						// Difference Rewards
						
						if (GesamtzeitPuffer[0][action_Wert] >=  KapazitaetMaschine[0][action_Wert]) {
							localreward = Maschinenzeitfaktor[0][action_Wert] * Math.exp(-1);
							localreward_1 = Maschinenzeitfaktor[0][action_Wert] * Math.exp(-1);
							
							reward = localreward-localreward_1; // faktisch 0
						}
						else {
							localreward = Maschinenzeitfaktor[0][action_Wert] * Math.exp((-1*GesamtzeitPuffer[0][action_Wert])/KapazitaetMaschine[0][action_Wert]);
							localreward_1 = Maschinenzeitfaktor[0][action_Wert] * Math.exp((-1*GesamtzeitPuffer_1[0][action_Wert])/KapazitaetMaschine[0][action_Wert]);
							
							reward = localreward - localreward_1;
						}
						*/
						
						if (state[0][action_Wert] == min_Bearbeitungszeit) {
							reward = 1;
						}
						
						else if (state[0][action_Wert] == max_Bearbeitungszeit) {
							reward = -1;
						}
						else
							reward = 0;
						
						
						if (aktuelle_Station == null) {
							aktuelle_Station = "Puffer"; // Verfahren anpassen: Wenn MAS nicht zurückgesetzt wird, ist aktuell Station zu Beginn nicht == null!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
						}
						
						if (aktuelle_Station == "Puffer") {
							state_t = Float.toString(state[0][0]);
							for (int i = 1; i < Umfang_BE_Gruppe; i++) { // Max-Wert für i bei Bedarf anpassen!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
								state_t = state_t + "," + Float.toString(state[0][i]);
							}
							a_t = action_Wert;
							r_t = reward;
						}
						else {
							state_t_ = Float.toString(state[0][0]);
							for (int i = 1; i < Umfang_BE_Gruppe; i++) {
								state_t_ = state_t_ + "," + Float.toString(state[0][i]);
							}
							//a_t = action_Wert; // Änderung 31.08.2020 - zurück geändert 02.09.20
							//r_t = Integer.parseInt(Auftragsdaten.get(10));
							//r_t = reward;
						}

						if (bestStation != null) {
							
							if (aktuelle_Station != "Puffer") {
								Write_local_memory app = new Write_local_memory();
							    app.insert(getLocalName(), state_t, state_t_, a_t, r_t, 1);
							    state_t = state_t_;
							    a_t = action_Wert;
							    r_t = reward;
							}
							
							ACLMessage Ergebnis_Belegungsplanung = new ACLMessage(ACLMessage.INFORM); // Wenn ja, dann Nachricht für die Rückmeldung des Ergebnisses der Belegungsplanung erstellen
							Ergebnis_Belegungsplanung.addReceiver(new AID("Socket_out", AID.ISLOCALNAME)); // Nachricht über Socket an Plant Simulation schicken
							Ergebnis_Belegungsplanung.setContent("B:" + getLocalName() + ":" + bestStation.getLocalName() + ":" + Bearbeitungszeit + ":" + aktuelle_Station + ";"); // Wenn ja, dann über den Inhalt die gewählte Maschine inklusive der notwendigen Daten an Plant Simulation melden (wird in Steuerung verarbeitet)
							send(Ergebnis_Belegungsplanung); // Versenden der Rückmeldung
							
							aktuelle_Station = bestStation.getLocalName(); // Speichern der Maschine, auf der sich der Auftrag ab sofort befindet
						}
						step++;
					}
				} else {
					block();
				}
			}
		}
		
		public boolean done() { // Rückmeldung, dass Behaviour abgeschlossen ist
			return step == 4;
		}
	}
}