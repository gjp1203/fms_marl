package dqn_functions;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.io.File;
 
public class Transfer_replay_memory_Maschine {
  
	static int Anzahl_steps = 0;
	private int Größe_Replay_Memory = 10000;
    
    public void Übertragung (String Name) {
        String sql_local = "SELECT id, state_t, state_t_ ,a_t, r_t, done_flag FROM Memories";
        
     // SQLite connection string
        String url_local = "jdbc:sqlite:./replay_memory_maschine/" + Name + ".db";
        Connection conn_local = null;
        try {
            conn_local = DriverManager.getConnection(url_local);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        
        try (Statement stmt  = conn_local.createStatement();
             ResultSet rs    = stmt.executeQuery(sql_local)){
            
            // loop through the result set
            while (rs.next()) {
            	
            	synchronized(this) { // Zählen der Steps und damit der Datensätze, die in den Replay Memory geschrieben wurden
                	Anzahl_steps++;
                }
            	
            	if (Anzahl_steps <= Größe_Replay_Memory) {
            		
            		String sql_global = "INSERT INTO Memories(state_t,state_t_,a_t,r_t,done_flag) VALUES(?,?,?,?,?)";
            		 
                	// SQLite connection string
                    String url_global = "jdbc:sqlite:./replay_memory_maschine/Replay_Memory_global.db";
                    Connection conn_global = null;
                    try {
                        conn_global = DriverManager.getConnection(url_global);
                    } catch (SQLException e) {
                        System.out.println(e.getMessage());
                    }
                	
                	try (PreparedStatement pstmt = conn_global.prepareStatement(sql_global)) {
                        pstmt.setString(1, rs.getString("state_t"));
                        pstmt.setString(2, rs.getString("state_t_"));
                        pstmt.setInt(3, rs.getInt("a_t"));
                        pstmt.setDouble(4, rs.getDouble("r_t"));
                        pstmt.setInt(5, rs.getInt("done_flag"));
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
                        pstmt.setString(1, rs.getString("state_t"));
                        pstmt.setString(2, rs.getString("state_t_"));
                        pstmt.setInt(3, rs.getInt("a_t"));
                        pstmt.setDouble(4, rs.getDouble("r_t"));
                        pstmt.setInt(5, rs.getInt("done_flag"));
                        pstmt.setInt(6, id);
                        // update 
                        pstmt.executeUpdate();
                    } catch (SQLException e) {
                        System.out.println(e.getMessage());
                    }                    
            	}
            }
            stmt.close();
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
        	try {
    			conn_local.close();
    		} catch (SQLException e1) {
    			e1.printStackTrace();
    		}
      
        	//lokalen Memory löschen
            try  {         
            	File f= new File("./replay_memory_maschine/" + Name + ".db"); // Pfad des jeweiligen lokalen Memories  
            	if(f.delete()) {  
            		System.out.println(f.getName() + ": lokaler Replay Memory gelöscht");  
            	}  
            	else {  
            		System.out.println(f.getName() + " failed");  
            	}  
            } catch(Exception e) {  
            	e.printStackTrace();  
            }
        }  
    }
    
    public static int getAnzahl_steps () {
    	return Anzahl_steps;
    }
}