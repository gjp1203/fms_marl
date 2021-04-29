package dqn_functions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
 
public class Clear_replay_memory {
 
    public void delete(String Name) {
        String sql = "DELETE FROM Memories";
        
        String url = "jdbc:sqlite:./replay_memory/" + Name + ".db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        try {
			conn.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
    }
}