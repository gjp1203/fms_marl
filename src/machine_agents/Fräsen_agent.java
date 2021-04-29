package machine_agents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.ArrayList;
import java.util.Arrays;

import org.tensorflow.Graph;

import communication_functions.Msg_serialization;
import dqn_functions.Initialization_replay_memory_Maschine;
import dqn_functions.Initialization_replay_memory;
import dqn_functions.Load_graph_Maschine;
import dqn_functions.Tensorflow_Maschine;
import dqn_functions.Transfer_replay_memory_Maschine;
import dqn_functions.Transfer_replay_memory;
import dqn_functions.Write_local_memory;
import dqn_functions.Write_local_memory_Maschine;
import debugging_functions.Start_dqn;
import debugging_functions.Start_dqn_maschine;
import debugging_functions.Start_dqn_maschine_100;

public class Fr�sen_agent extends Agent {
	
//--------------------------Initalisierung des Maschinenagentens------------------------------------------------------------------------------------------------	
	
	ArrayList<String> Stammdaten = new ArrayList<String>(); // Verfahren:L�nge:Durchmesser:Tiefe:Achsenzahl:Material:Maschinen-:Al-:Stahl-:Ti-Zeitfaktor
	ArrayList<String> Bewegungsdaten = new ArrayList<String>(); // Verf�gbarkeit:ZeitbisBearbeitung:Status:MTTR:Anzahl_Auftr�ge_in_Puffer:Anzahl_Auftragstypen_in_Puffer
	private Graph g;
	private String state_t;
	private String state_t_;
	private int a_t;
	private double r_t;
	static int Anzahl_Aufrufe = 0;
	
	public void setup() {
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Fr�sen"); // Festlegung des individuellen Prozesses
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		/*
		try {
			g = Load_graph_Maschine.graph();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		Initialization_replay_memory_Maschine.createNewDatabase(getLocalName()); // Initialisierung des lokalen Replay Memories f�r Reihenfolgebildung 
		Initialization_replay_memory.createNewDatabase(getLocalName());			// F�r Belegungsplanung 
		
		addBehaviour(new Maschinensteuerung()); // Initialisierung des Cyclic Behaviours f�r das Aufrufen der Steuerungsfunktionen
		
		System.out.println(getLocalName()+" initialisiert");
	}
	
//--------------------------Cyclic Behaviour f�r das Aufrufen der Steuerungsfunktionen--------------------------------------------------------------------------
	
	private class Maschinensteuerung extends CyclicBehaviour {
		
		private MessageTemplate Maschinendaten_Nachricht; // Wird durch Datenerfassung, Datenerfassung1 und Ende_Zeit in Plant Simulation genutzt
		//private AID targetStation = null;
		
		public void action() {
			
			Maschinendaten_Nachricht = MessageTemplate.MatchPerformative(ACLMessage.INFORM); // Definition von Nachrichten, die den aktuellen Zustand des Auftrags beschreiben
			ACLMessage Maschinendaten = receive(Maschinendaten_Nachricht);
			
			
			if (Maschinendaten != null) {
				
				if (Maschinendaten.getConversationId().equals("S") == true) {
					
					String Nachricht_Inhalt = Maschinendaten.getContent(); // Variablen f�r das Auslesen der Nachricht
					String Zwischenspeicher = "";
					char aChar;
					int Datenfeld = 0;
					Stammdaten.clear(); // Notwendig, damit aktualisierte Stammdaten an die richtige Stelle und nicht an das Ende der ArrayList geschrieben werden
					System.out.println("Stammdaten!!!");
					for (int i = 0; i < Nachricht_Inhalt.length(); i++) { // Jedes Zeichen der Nachricht wird einzeln gepr�ft
						
						aChar = Nachricht_Inhalt.charAt(i);
						
						if (aChar == ';') { // Pr�fen, ob Ende der Nachricht erreicht
							return;
						}
						else if (aChar == ':') { //Pr�fen, ob Ende des jeweiligen Datums erreicht
							Datenfeld++; // Wenn ja, dann neues Datenfeld markieren
							Zwischenspeicher = ""; // und Zwischenspeicher f�r dieses Datenfeld zur�cksetzen
						}
						else if (aChar != ';' && aChar != ':') { // Pr�fen, ob aktuell Daten gelesen werden
							Zwischenspeicher = Zwischenspeicher + aChar; // Wenn ja, dann aktuelles Zeichen in Zwischenspeicher schreiben
							if (Stammdaten.size() < Datenfeld+1) { // Pr�fen, ob bereits mit dem Schreiben des Stammdatums in das jeweilige Feld begonnen wurde
								Stammdaten.add(Zwischenspeicher); // Wenn nicht, dann Feld anlegen und mit Schreiben beginnen
							}
							else if (Stammdaten.size() >= Datenfeld+1) { // Schreiben des Stammdatums in das jeweilige Feld wurde bereits begonnen
								Stammdaten.set(Datenfeld, Zwischenspeicher);// Schreiben fortsetzen
							}
						}
					}
				}
				
				else if (Maschinendaten.getConversationId().equals("M")) {
					String Nachricht_Inhalt = Maschinendaten.getContent(); // Variablen f�r das Auslesen der Nachricht
					String Zwischenspeicher = "";
					char aChar;
					int Datenfeld = 0;
					Bewegungsdaten.clear(); // Notwendig, damit aktualisierte Bewegungsdaten an die richtige Stelle und nicht an das Ende der ArrayList geschrieben werden
						
					for (int i = 0; i < Nachricht_Inhalt.length(); i++) { // Jedes Zeichen der Nachricht wird einzeln gepr�ft
								
						aChar = Nachricht_Inhalt.charAt(i);
								
						if (aChar == ';') { // Pr�fen, ob Ende der Nachricht erreicht
							return;
						}
						else if (aChar == ':') { //Pr�fen, ob Ende des jeweiligen Datums erreicht
							Datenfeld++; // Wenn ja, dann neues Datenfeld markieren
							Zwischenspeicher = ""; // und Zwischenspeicher f�r dieses Datenfeld zur�cksetzen
						}
						else if (aChar != ';' && aChar != ':') { // Pr�fen, ob aktuell Daten gelesen werden
							Zwischenspeicher = Zwischenspeicher + aChar; // Wenn ja, dann aktuelles Zeichen in Zwischenspeicher schreiben
							if (Bewegungsdaten.size() < Datenfeld+1) { // Pr�fen, ob bereits mit dem Schreiben des Bewegungsdatums in das jeweilige Feld begonnen wurde
								Bewegungsdaten.add(Zwischenspeicher); // Wenn nicht, dann Feld anlegen und mit Schreiben beginnen
							}
							else if (Bewegungsdaten.size() >= Datenfeld+1) { // Schreiben des Bewegungsdatums in das jeweilige Feld wurde bereits begonnen
								Bewegungsdaten.set(Datenfeld, Zwischenspeicher);// Schreiben fortsetzen
							}
						}
					}
				}
				
				else if (Maschinendaten.getConversationId().equals("B")) {
					java.io.Serializable Inhalt = null; // Deserialisierung des Inhalts �ber Klasse Msg_serialization
					try {
						Inhalt = Maschinendaten.getContentObject();
					}
					catch (UnreadableException e) {
						e.printStackTrace();
					}
					
					ACLMessage R�ckmeldung = Maschinendaten.createReply(); // Erstellen der Antwort auf die Datenanfrage f�r die Belegungsplanung des Auftrags
					
					if (((Msg_serialization) Inhalt).getL�nge() > Integer.parseInt(Stammdaten.get(1)) || // Pr�fen, ob Widerspruch Zwischen Auftrags- und Maschinenstammdaten besteht
						((Msg_serialization) Inhalt).getDurchmesser() > Integer.parseInt(Stammdaten.get(2)) || // Vergleich der Stammdaten aus ArrayList mit deserialisierten Auftragsdaten aus Anfrage des Auftrags
						((Msg_serialization) Inhalt).getTiefe() > Integer.parseInt(Stammdaten.get(3)) ||
						((Msg_serialization) Inhalt).getAchsenzahl() > Integer.parseInt(Stammdaten.get(4)) ||
						!((Msg_serialization) Inhalt).getMaterial().equals(Stammdaten.get(5))) {
						R�ckmeldung.setPerformative(ACLMessage.REFUSE); // Maschinenagent meldet Nichtverf�gbarkeit f�r Belegungsplanung zur�ck
					}
					
					else if (((Msg_serialization) Inhalt).getL�nge() <= Integer.parseInt(Stammdaten.get(1)) && // Maschine kann Auftrag bearbeiten
							((Msg_serialization) Inhalt).getDurchmesser() <= Integer.parseInt(Stammdaten.get(2)) &&
							((Msg_serialization) Inhalt).getTiefe() <= Integer.parseInt(Stammdaten.get(3)) &&
							((Msg_serialization) Inhalt).getAchsenzahl() <= Integer.parseInt(Stammdaten.get(4)) &&
							((Msg_serialization) Inhalt).getMaterial().equals(Stammdaten.get(5))) {
						R�ckmeldung.setPerformative(ACLMessage.PROPOSE); // Maschinenagent meldet Daten zur m�glichen Bearbeitung zur�ck
						
						Msg_serialization ms = new Msg_serialization(); // Serialisierung der Bewegungsdaten �ber Klasse Msg_serialization
						   ms.setVerf�gbarkeit(Integer.parseInt(Bewegungsdaten.get(0)));
						   ms.setZeitbisBearbeitung(Float.parseFloat(Bewegungsdaten.get(1)));
						   ms.setStatus(Bewegungsdaten.get(2));
						   ms.setMTTR(Integer.parseInt(Bewegungsdaten.get(3)));
						   ms.setGesamtzeitPuffer(Float.parseFloat(Bewegungsdaten.get(6)));
						   ms.setGesamtzeitPuffer_1(Float.parseFloat(Bewegungsdaten.get(7)));
						   ms.setKapazitaetMaschine(Float.parseFloat(Bewegungsdaten.get(8)));
						   
						   
						if (((Msg_serialization) Inhalt).getMaterial().equals("Aluminium")) { // Pr�fen, aus welchem Material der Auftrag besteht
							ms.setBearbeitungszeitfaktor(Float.parseFloat(Stammdaten.get(6))*Float.parseFloat(Stammdaten.get(7))); // Entsprechend Zeitfaktor berechnen (abh�ngig von Maschine und Material)
						}
						if (((Msg_serialization) Inhalt).getMaterial().equals("Stahl")) {
							ms.setBearbeitungszeitfaktor(Float.parseFloat(Stammdaten.get(6))*Float.parseFloat(Stammdaten.get(8)));
						}
						if (((Msg_serialization) Inhalt).getMaterial().equals("Titan")) {
							ms.setBearbeitungszeitfaktor(Float.parseFloat(Stammdaten.get(6))*Float.parseFloat(Stammdaten.get(9)));
						}
						   
						try {
							  R�ckmeldung.setContentObject(ms); // Serialisierte Daten als Inhalt der R�ckmeldung festlegen
							}
							catch (Exception ex) {
								ex.printStackTrace();
							}
					}
					send(R�ckmeldung); // Versenden der R�ckmeldung
				}
				
				else if (Maschinendaten.getConversationId().equals("E")) {
					
					r_t = Float.parseFloat(Maschinendaten.getContent());
					
					state_t_ = null;
					for (int i = 0; i < 10; i++) {
						state_t_ = state_t_ + "," + "0,0,0,0";
					}
					
					Write_local_memory app_1 = new Write_local_memory();
					app_1.insert(getLocalName(),state_t, state_t_, a_t, r_t, 0);
			    
					Write_local_memory_Maschine app_2 = new Write_local_memory_Maschine();
					app_2.insert(state_t, state_t_, a_t, r_t, 0);
					//Transfer_replay_memory app_2 = new Transfer_replay_memory();
					//app_2.�bertragung(getLocalName());
					
				}
			
				else if (Maschinendaten.getConversationId().equals("Z")) // theoretisch uninteressant: deswegen von R auf Z (dann sollte es nie aufgerufen werden)
				
					try{
					
						float[][] state = new float[1][100]; // Zustandsvektor f�r das Deep q-Learning --> "MUST Provide as many biases as the Input"
						String Nachricht_Inhalt = Maschinendaten.getContent(); // Variablen f�r das Auslesen der Nachricht
						String Zwischenspeicher = "";
						char aChar;
						int Datenfeld = 0;
					
						Anzahl_Aufrufe++;
						System.out.println(Anzahl_Aufrufe);
					
						if (Anzahl_Aufrufe == Tensorflow_Maschine.get_warm_up_steps()) {
							//System.out.println("DQN starten!");
							//Start_dqn_maschine_100.Start_dqn_function();							// Start DQN eingef�gt von Kemp
							
						}
						
						/* Ausgelagert in Tensorflow_Maschine
						if (Transfer_replay_memory_Maschine.getAnzahl_steps() % 10 == 0) {
							try {
								g = Load_graph_Maschine.graph();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}*/
						
						for (int i = 0; i < Nachricht_Inhalt.length(); i++) { // Jedes Zeichen der Nachricht wird einzeln gepr�ft
							//System.out.println(Nachricht_Inhalt.length() + "!!!!!!!!!!!!!!!!!!!!!!!");
							//System.out.println(Nachricht_Inhalt + "!!!!!!!!!!!!!!!!!!!!!!!");
							aChar = Nachricht_Inhalt.charAt(i);
										
							if (aChar == ';') { // Pr�fen, ob Ende der Nachricht erreicht
								return;
							}
							else if (aChar == ':') { //Pr�fen, ob Ende des jeweiligen Datums erreicht
								if (Datenfeld >= 1) {
									state[0][Datenfeld-1] = Float.parseFloat(Zwischenspeicher);
								}
								Datenfeld++; // Wenn ja, dann neues Datenfeld markieren
								Zwischenspeicher = ""; // und Zwischenspeicher f�r dieses Datenfeld zur�cksetzen
							}
							else if (aChar != ';' && aChar != ':' && Datenfeld == 0) { // Pr�fen, ob aktuell Daten gelesen werden
								Zwischenspeicher = Zwischenspeicher + aChar; // Wenn ja, dann aktuelles Zeichen in Zwischenspeicher schreiben
								if (Bewegungsdaten.size() < Datenfeld+7) { // Pr�fen, ob bereits mit dem Schreiben des Auftragsdatums in das jeweilige Feld begonnen wurde (+6, da erste 5 Daten durch Verf�gbarkeitspr�fung geschrieben werden)
									Bewegungsdaten.add(Zwischenspeicher); // Wenn nicht, dann Feld anlegen und mit Schreiben beginnen
								}
								else if (Bewegungsdaten.size() >= Datenfeld+7) { // Schreiben des Auftragsdatums in das jeweilige Feld wurde bereits begonnen
									Bewegungsdaten.set(Datenfeld+6, Zwischenspeicher);// Schreiben fortsetzen
								}
							}
							else if (aChar != ';' && aChar != ':' && Datenfeld >= 1) {
								Zwischenspeicher = Zwischenspeicher + aChar;
								//state[0][Datenfeld-1] = Integer.parseInt(Zwischenspeicher); // Bearbeitungszeit f�r den aktuellen Auftrag auf der jeweiligen Maschine berechnen
								//System.out.println(Arrays.deepToString(state));
							}
						}
						
						System.out.println("Zustandsvektor Maschine: " + Arrays.deepToString(state));
						
						int action_Wert = Tensorflow_Maschine.action(state);
						//targetStation = new AID(action[action_Wert],aid.isLocalName);
						
						
						if (state_t == null) {
							state_t = Float.toString(state[0][0]);
							for (int i = 1; i < Datenfeld; i++) {
								state_t = state_t + "," + Float.toString(state[0][i]);
							}
							a_t = action_Wert;
							//r_t = Float.parseFloat(Auftragsdaten.get(9));
						}
						
						else {
							state_t_ = Float.toString(state[0][0]);
							for (int i = 1; i < Datenfeld; i++) {
								state_t_ = state_t_ + "," + Float.toString(state[0][i]);
							}
							r_t = Float.parseFloat(Bewegungsdaten.get(6));
							
							Write_local_memory app = new Write_local_memory();
						    app.insert(getLocalName(),state_t, state_t_, a_t, r_t, 1);
						    //state_t = state_t_;
						    //a_t = action_Wert;
						    
						    // Doppelte Dokumentation 
						    Write_local_memory_Maschine app1 = new Write_local_memory_Maschine();
						    app1.insert(state_t, state_t_, a_t, r_t, 1);
						    state_t = state_t_;
						    a_t = action_Wert;
						}
						
						//tragetStation = AID.getLocalName();
						//System.out.println (getLocalName());
						ACLMessage Ergebnis_Belegungsplanung = new ACLMessage(ACLMessage.INFORM); // Wenn ja, dann Nachricht f�r die R�ckmeldung des Ergebnisses der Belegungsplanung erstellen
						Ergebnis_Belegungsplanung.addReceiver(new AID("Socket_out", AID.ISLOCALNAME)); // Nachricht �ber Socket an Plant Simulation schicken
						Ergebnis_Belegungsplanung.setContent("R:" + action_Wert + ":" + getLocalName() + ";"); // Wenn ja, dann �ber den Inhalt die gew�hlte Maschine inklusive der notwendigen Daten an Plant Simulation melden (wird in Steuerung verarbeitet)
						send(Ergebnis_Belegungsplanung); // Versenden der R�ckmeldung
						
						g = null;
						state =null; 
						System.gc();
				} catch (Exception e) {
					e.printStackTrace();
					}
				}
			else {
				block();
			}
		}
	}
}
