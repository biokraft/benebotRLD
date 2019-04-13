import java.sql.SQLException;
import java.util.TimerTask;

public class DeleteTask extends TimerTask {
    public void run() {
        try {
            Database.deleteAllTriggersInProcess();
            BotController.resetTriggersInProcess();
        } catch (SQLException ex) {
            System.out.println("Error deleting triggers in process " + ex.getMessage());
        }
    }
}
