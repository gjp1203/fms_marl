package debugging_functions;
import java.io.*;

import supervisor_agents.Initialization_agent;


public class Start_dqn {
	
	public static void Start_dqn_function() {
	    
		try {
	    	  
	    	  String current = new java.io.File(".").getCanonicalPath();
	    	  System.out.println(current);
	    	  
	    	 
	    	  String pythonPath = current + "\\dqn_agent";
	    	  System.out.println(pythonPath);
	    	  switch(Initialization_agent.Durchlauf) {
	    	  case 1:
	    		  File pythonfile1 = new File(pythonPath + "main_dqn_1.py");
		    	  
		    	  String command1 = "cmd.exe /k start python dqn_agent\\main_dqn_1.py"; 
		    	  Process p1 = Runtime.getRuntime().exec(command1);
		    	  break;
	    	  case 2:
	    		  File pythonfile2 = new File(pythonPath + "main_dqn_2.py");
		    	  
		    	  String command2 = "cmd.exe /k start python dqn_agent\\main_dqn_2.py"; 
		    	  Process p2 = Runtime.getRuntime().exec(command2);
		    	  break;
	    	  case 3:
	    		  File pythonfile3 = new File(pythonPath + "main_dqn_3.py");
		    	  
		    	  String command3 = "cmd.exe /k start python dqn_agent\\main_dqn_3.py"; 
		    	  Process p3 = Runtime.getRuntime().exec(command3);
		    	  break;
	    	  case 4:
	    		  File pythonfile4 = new File(pythonPath + "main_dqn.py");
		    	  
		    	  String command4 = "cmd.exe /k start python dqn_agent\\main_dqn_4.py"; 
		    	  Process p4 = Runtime.getRuntime().exec(command4);
		    	  break;
	    	  default:
	    		  File pythonfile = new File(pythonPath + "main_dqn.py");
		    	  
		    	  String command = "cmd.exe /k start python dqn_agent\\main_dqn.py"; 
		    	  Process p = Runtime.getRuntime().exec(command);
		    	  break;

	    	  }
	    	  /*
	    	  File pythonfile = new File(pythonPath + "main_dqn.py");
	    	  
	    	  String command = "cmd.exe /k start python dqn_agent\\main_dqn.py"; 
	    	  Process p = Runtime.getRuntime().exec(command);
	    	  */
	    	  
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
	    	  
	    	  
	    	  
	    	  
	    	  
	    	  
	