package debugging_functions;
import java.io.*;


public class Start_dqn_maschine {
	
	public static void Start_dqn_function() {
	    
		try {
	    	  
	    	  String current = new java.io.File(".").getCanonicalPath();
	    	  System.out.println(current);
	    	  
	    	 
	    	  String pythonPath = current + "\\dqn_agent";
	    	  System.out.println(pythonPath);
	    	  
	    	  File pythonfile = new File(pythonPath + "main_dqn.py");
	    	  
	    	  String command = "cmd.exe /k start python dqn_agent\\main_dqn_maschine.py"; 
	    	  Process p = Runtime.getRuntime().exec(command);
	    	  
	    	  
	    	  //ProcessBuilder pb = new ProcessBuilder("python",pythonPath);
	    	  //ProcessBuilder pb = new ProcessBuilder("python",pythonfile.getAbsolutePath());
	    	  //Process p = pb.start();   	  
	    	 
	    	  
	    	  //Process process = Runtime.getRuntime().exec("python ./dqn_agent/main_dqn.py");
	    	  
	    	  
	    	
	    	  //ProcessBuilder pb = new ProcessBuilder();
	    	  //pb.command("cmd","/c", "start", "dir %s").directory(new File(pythonPath));
	    	  //pb.command("cmd","/c", "start", "cd %s").directory(new File(pythonPath));
	    	 
	    	
	    	  
	    	  //Process p = pb.start();
	    	  //Runtime.getRuntime().exec("python main_dqn.py");
	    	  
	    	  //Process process = Runtime.getRuntime().exec("cmd.exe /c start dir %s",null, new File(pythonPath),null,"python main_dqn.py");
	    	
	    	  //ProcessBuilder pb = new ProcessBuilder("start", "cmd.exe");
	    	  //Process p = pb.start();
	    	  
	    	  
	    	  /*
	    	  ProcessBuilder pb = new ProcessBuilder("python",pythonPath);
	    	  Process p = pb.start();
	    	  
	    	  BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    	  String pythonLine = new String(in.readLine());
	    	  System.out.println(pythonLine);
	    	  */
	      }
	     catch (Exception e ) {System.out.println(e);}
	}
}
	    	  
	    	  
	    	  
	    	  
	    	  
	    	  
	