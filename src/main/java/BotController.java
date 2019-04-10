import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class BotController extends TelegramLongPollingBot {

  public void onUpdateReceived(Update update) {

  }

  public String getBotUsername() {
    return "Benebot 3.0";
  }

  public String getBotToken() {
    return "529232672:AAHxsYaI8WvGwbXs5jz4v1t2F4KIB1aTFoU";
  }
}
