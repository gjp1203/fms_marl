package dqn_functions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Initialization_replay_memory {
 // Vorschlag Kevin: 
	private static final String sql = "CREATE TABLE IF NOT EXISTS Memories (\n"
			+ "    id integer PRIMARY KEY,\n"
            + "    state_t text NOT NULL,\n"
            + "    state_t_ text NOT NULL,\n"
            + "    a_t integer,\n"
            + "    r_t real,\n"
            + "    done_flag real\n"
            + ");";
	
	// Erstellung einer neuen Datenbank 
	public static void createNewDatabase(String Name) {
		String url = "jdbc:sqlite:./replay_memory/" + Name + ".db";
		Connection conn=null;
		
		try {
			conn = DriverManager.getConnection(url);
			Statement stmt = conn.createStatement();
			
			System.out.println("Replay Memory initialisiert");
			
			// Erstellung einer neuen Tabelle innerhalb der Datenbank
			
			stmt.execute(sql);
			
			Clear_replay_memory app = new Clear_replay_memory(); // Löschen bestehender Daten, falls Datenbank aus einem vorherigen Lauf übernommen wird
	        app.delete(Name);
		}
		catch (SQLException e) {
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
	
	
	
	
	
//--------------------------Erstellen einer neuen Datenbank------------------------------------------------------------------------------------------------
	
 /*   public static void createNewDatabase(String Name) {
 
        String url = "jdbc:sqlite:./replay_memory/" + Name + ".db";
        Connection conn = null;
 
        try {
        	conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
       
        System.out.println("Replay Memory initialisiert");

        try {
			conn.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
 
//--------------------------Erstellen einer neuen Tabelle innerhalb der Datenbank--------------------------------------------------------------------------
    
        String sql = "CREATE TABLE IF NOT EXISTS Memories (\n"
                + "    id integer PRIMARY KEY,\n"
                + "    state_t text NOT NULL,\n"
                + "    state_t_ text NOT NULL,\n"
                + "    a_t integer,\n"
                + "    r_t real,\n"
                + "    done_flag real\n"
                + ");";
        
        try {
        	conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
        	System.out.println(e.getMessage());
        }
        
        try (Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        
        try {
			conn.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
        
        Clear_replay_memory app = new Clear_replay_memory(); // Löschen bestehender Daten, falls Datenbank aus einem vorherigen Lauf übernommen wird
        app.delete(Name);
    }
}*/