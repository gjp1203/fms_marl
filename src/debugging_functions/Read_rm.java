package debugging_functions;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Read_rm {
 
    private Connection connect() {
        // SQLite connection string
        String url = "jdbc:sqlite:./replay_memory/Replay_Memory_global.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }
 
    public void selectAll(){
        String sql = "SELECT id, state_t, state_t_, a_t, r_t, done_flag FROM Memories";
        
        try (Connection conn = this.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){
            
            // loop through the result set
            while (rs.next()) {
                System.out.println(rs.getInt("id") +  "\t" + 
                                   rs.getString("state_t") + "\t" +
                                   rs.getString("state_t_") + "\t" +
                                   rs.getInt("a_t") + "\t" +
                                   rs.getDouble("r_t") + "\t" +
                                   rs.getInt("done_flag"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}