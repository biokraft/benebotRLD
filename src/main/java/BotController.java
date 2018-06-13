import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

public class BotController extends TelegramLongPollingBot {

  public void onUpdateReceived(Update update) {

  }

  public String getBotUsername() {
    return "Benebot 2.0";
  }

  public String getBotToken() {
    return "519412710:AAHvfawmwzPCSZtmImFDWS870qsudwrZHBA";
  }
}
