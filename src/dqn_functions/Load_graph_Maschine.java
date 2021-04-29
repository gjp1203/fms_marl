package dqn_functions;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.tensorflow.Graph;

public class Load_graph_Maschine {
	private final static int MAX_RETRY_COUNT=5;
	private static int retryCount = 1;
	
	public static Graph graph() throws URISyntaxException, IOException {
			Graph g = new Graph();
			while (true) {
				
				try {
				Path modelPath = Paths.get("./dqn_agent/function_approximator_maschine/saved_network.pb");
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