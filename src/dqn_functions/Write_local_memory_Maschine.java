package dqn_functions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
 
public class Write_local_memory_Maschine {
	
	static int Anzahl_steps = 0;
	private int Größe_Replay_Memory = 1000;
	
    public void insert(String state_t, String state_t_, int a_t, double r_t, int done_flag) {
    	
    	synchronized(this) { // Zählen der Steps und damit der Datensätze, die in den Replay Memory geschrieben wurden
        	Anzahl_steps++;
        }
    	
    	Connection conn = null;
    	
    	try {
    		
    		//SQLite connection string
    		String url = "jdbc:sqlite:./replay_memory_maschine/Replay_Memory_global.db";
    		
    		if (Anzahl_steps <= Größe_Replay_Memory) {
    			
    			String sql = "INSERT INTO Memories(state_t,state_t_,a_t,r_t,done_flag) VALUES(?,?,?,?,?)";
    			conn = DriverManager.getConnection(url);
    			
    			try (PreparedStatement pstmt = conn.prepareStatement(sql)){
    				pstmt.setString(1, state_t);
                    pstmt.setString(2, state_t_);
                    pstmt.setInt(3, a_t);
                    pstmt.setDouble(4, r_t);
                    pstmt.setInt(5, done_flag);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
    			
    		}
    		else if (Anzahl_steps > Größe_Replay_Memory) {
    			
    			String sql_global = "UPDATE Memories SET state_t = ? , "
                        + "state_t_ = ?, "
                        + "a_t = ?, "
                        + "r_t = ?, "
                        + "done_flag = ? "
                        + "WHERE id = ?";
        		
        		int id = Anzahl_steps % Größe_Replay_Memory;
        		conn = DriverManager.getConnection(url);
        		 
	        		try (PreparedStatement pstmt = conn.prepareStatement(sql_global)) {
	        		     
	                     // set the corresponding param
	                     pstmt.setString(1, state_t);
	                     pstmt.setString(2, state_t_);
	                     pstmt.setInt(3, a_t);
	                     pstmt.setDouble(4, r_t);
	                     pstmt.setInt(5, done_flag);
	                     pstmt.setInt(6, id);
	                     // update 
	                     pstmt.executeUpdate();
	                 } catch (SQLException e) {
	                     System.out.println(e.getMessage());
	                 }
    		}
    		
    	}
    	catch (SQLException e){
    		System.out.println(e.getMessage());
    	}
    	finally {
    		try {
				conn.close();
			}
			catch (SQLException e1) {
				e1.printStackTrace();
			}
    	}
    }
}
    /*
    	if (Anzahl_steps <= Größe_Replay_Memory) {
    		String sql = "INSERT INTO Memories(state_t,state_t_,a_t,r_t,done_flag) VALUES(?,?,?,?,?)";
    		 
        	// SQLite connection string
            String url = "jdbc:sqlite:./replay_memory_maschine/Replay_Memory_global.db";
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(url);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        	
        	try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, state_t);
                pstmt.setString(2, state_t_);
                pstmt.setInt(3, a_t);
                pstmt.setDouble(4, r_t);
                pstmt.setInt(5, done_flag);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
    	}
    	
    	else if (Anzahl_steps > Größe_Replay_Memory) {
    		String sql_global = "UPDATE Memories SET state_t = ? , "
                    + "state_t_ = ?, "
                    + "a_t = ?, "
                    + "r_t = ?, "
                    + "done_flag = ? "
                    + "WHERE id = ?";
    		
    		int id = Anzahl_steps % Größe_Replay_Memory;
    		
    		// SQLite connection string
            String url_global = "jdbc:sqlite:./replay_memory_maschine/Replay_Memory_global.db";
            Connection conn_global = null;
            try {
                conn_global = DriverManager.getConnection(url_global);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
    		
            try (PreparedStatement pstmt = conn_global.prepareStatement(sql_global)) {
     
                // set the corresponding param
                pstmt.setString(1, state_t);
                pstmt.setString(2, state_t_);
                pstmt.setInt(3, a_t);
                pstmt.setDouble(4, r_t);
                pstmt.setInt(5, done_flag);
                pstmt.setInt(6, id);
                // update 
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
    	}
    	
    }
}*/