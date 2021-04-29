package dqn_functions;
import org.tensorflow.*;

import debugging_functions.Start_dqn_maschine_100;
import supervisor_agents.Initialization_agent;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.lang.Math;

public class Tensorflow_Maschine {
	
private static int warm_up_steps = 10000;
private final static int MAX_RETRY_COUNT=5;

public static int action(float[][] state) { // graph g hier aufrufen lassen
	
	int retryCount = 1;	// Für Fehlerschleife, falls Graph nicht geladen wird.
	
	Graph g = null;
	double epsilon_Beginn = 0.4;
	double epsilon_Ende = 0.01;
	double epsilon_Zerfall = 0.999;
	int output_dim = 100;				
	int a_t = 0;

	
	while (true) 
	{
		try {
				if (Transfer_replay_memory.getAnzahl_steps() == warm_up_steps && Initialization_agent.StartDqnMaschine == false) {
					Start_dqn_maschine_100.Start_dqn_function();
					Initialization_agent.StartDqnMaschine = true;
				}
			
				if (Transfer_replay_memory_Maschine.getAnzahl_steps() >= Tensorflow_Maschine.get_warm_up_steps()) {
					g = Load_graph_Maschine.graph();
				}
	
				if (g == null|| Transfer_replay_memory_Maschine.getAnzahl_steps() <= warm_up_steps) {
					Random action_t = new Random();
					a_t = action_t.nextInt(30)+1;	// Eindämmen des Entscheidungsraums 
					//a_t = action_t.nextInt(output_dim)+1; // zufällige Aktion aus Aktionsvektor auswählen
					break;
				}
	
				else {
					double epsilon_aktuell = epsilon_Beginn * Math.pow(epsilon_Zerfall, Transfer_replay_memory_Maschine.getAnzahl_steps()); 
	
						if (epsilon_aktuell < epsilon_Ende) {
							epsilon_aktuell = epsilon_Ende;
						}
	
					Random eps = new Random();
					Random action_t = new Random();

					double Whs = eps.nextFloat();
					System.out.println("Whs_Maschine:" + Whs + "Epsilon_aktuell_Maschine:" + epsilon_aktuell);
		
					if (Whs <= epsilon_aktuell) { // Exploration
						a_t = action_t.nextInt(output_dim)+1; // zufällige Aktion aus Aktionsvektor auswählen
					}
					
					else if (Whs > epsilon_aktuell) {
			
						System.out.println(Arrays.deepToString(state) + ":::::::::::::::::::::::");
							try (Session sess = new Session(g)) {
									System.out.println("Try Block gestartet");
									Tensor inputTensor = Tensor.create(state, Float.class); //Formatierung des Zustandsvektors als Tensor
									float[][] output = predict(sess, inputTensor);
									System.out.println(Arrays.deepToString(output) + "??????????????????????????????????????????????????????????????????????????????????????????????");
									
									inputTensor = null;
									
									// Interpretation des Aktionsvektors            
									double max_Output = 0;
									a_t = 0;
										for (int i = 0; i < output[0].length; i++) { // Aktion mit dem höchsten Ausgabewert im Aktionsvektor bestimmen
											if (max_Output < output[0][i]) {
												max_Output = output[0][i];
												a_t = i;
											}
										}
						System.out.println(a_t + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						sess.close();
						break;
					}
				}
			}
		}
		// Falls ein Fehler im Graphen vorhanden ist, kann dieser bis zu 5 mal geladen werden
		catch (Exception e){
			if (retryCount > MAX_RETRY_COUNT) {
				e.printStackTrace();
				throw new RuntimeException ("Tensorflow Error (MAX RETRY = 5)", e);
			}
			
			try {
				TimeUnit.SECONDS.sleep(1);
			}
			catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
			
			retryCount++;
			continue;
		}
	}
	g = null; 
	System.gc();
	return a_t; // Gewählte Aktion wird an den Auftragsagenten zurückgegeben
}

private static float[][] predict(Session sess, Tensor inputTensor) {
    Tensor result = sess.runner()
    		.feed("q_eval/input_layer/MatMul", inputTensor)
            .fetch("q_eval/output_layer/BiasAdd").run().get(0);
    float[][] outputBuffer = new float[1][100];
    result.copyTo(outputBuffer);
    result = null;
    return outputBuffer;
}

public static int get_warm_up_steps () {
	return warm_up_steps;
}
}