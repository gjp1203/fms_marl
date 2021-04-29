package dqn_functions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
 
public class Write_local_memory {
	
    public void insert(String Name, String state_t, String state_t_, int a_t, double r_t, int done_flag) {
    	
    	String sql = "INSERT INTO Memories(state_t,state_t_,a_t,r_t,done_flag) VALUES(?,?,?,?,?)";
 
    	// SQLite connection string
        String url = "jdbc:sqlite:./replay_memory/" + Name + ".db";
        Connection conn = null;
        
        try {
            conn = DriverManager.getConnection(url);
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, state_t);
            pstmt.setString(2, state_t_);
            pstmt.setInt(3, a_t);
            pstmt.setDouble(4, r_t);
            pstmt.setInt(5, done_flag);
            pstmt.executeUpdate();
  
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        finally {
        	
        	try {
    			conn.close();
    		} catch (SQLException e) {
    			e.printStackTrace();
    		}
        	conn = null;
        }
    	/*
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
        
    	try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}*/
    }
}