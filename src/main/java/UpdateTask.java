import java.io.IOException;
import java.util.TimerTask;

public class UpdateTask extends TimerTask {

    public void run() {
        System.out.println("Updating trigger data...");
        BotController.updateTriggers();
    }
}
