import java.sql.SQLException;
import java.util.TimerTask;

public class DeleteTask extends TimerTask {
    public void run() {
        try {
            System.out.println("Deleting all triggers in process...");
            Database.deleteAllTriggersInProcess();
            BotController.resetTriggersInProcess();
        } catch (SQLException ex) {
            System.out.println("Error deleting triggers in process " + ex.getMessage());
        }
    }
}
