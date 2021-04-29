package dqn_functions;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.tensorflow.Graph;

import supervisor_agents.Initialization_agent;

public class Load_graph {
	private final static int MAX_RETRY_COUNT=20;
	private static int retryCount = 1;
	
	public static Graph graph() throws URISyntaxException, IOException {
		
			Graph g = new Graph();
			while (true) {
				
				try {
				Path modelPath;
				switch(Initialization_agent.Durchlauf) {
				case 1:
					modelPath = Paths.get("./dqn_agent/function_approximator/saved_network_1.pb");
					break;
				case 2: 
					modelPath = Paths.get("./dqn_agent/function_approximator/saved_network_2.pb");
					break;
				case 3: 
					modelPath = Paths.get("./dqn_agent/function_approximator/saved_network_3.pb");
					break;
				case 4:
					modelPath = Paths.get("./dqn_agent/function_approximator/saved_network_4.pb");
					break;
				default: 
					modelPath = Paths.get("./dqn_agent/function_approximator/saved_network.pb");
					break;
				}
			
				
				byte[] graph = Files.readAllBytes(modelPath);
				g.importGraphDef(graph);
				break;
				} catch (Exception e) {
					if (retryCount > MAX_RETRY_COUNT) {
						e.printStackTrace();
						throw new RuntimeException ("Tensorflow Error (MAX RETRY = 5)", e);
					}
						try {
							TimeUnit.MILLISECONDS.sleep(50);
						}
						catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
						}
					retryCount++;
					continue;
				}	
			}
			return g;
		}
}