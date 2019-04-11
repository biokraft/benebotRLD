import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BotController extends TelegramLongPollingBot {

  private static boolean developerMode = false;

  public void onUpdateReceived(Update update) {
      if (update.hasMessage() && update.getMessage().hasText()) {
          SendMessage sendMessage = new SendMessage()
                  .setChatId(update.getMessage().getChatId())
                  .setText(String.valueOf(update.getMessage().getFrom().getId())
                  + "\n" + update.getMessage().getChatId());
          try {
              execute(sendMessage);
          } catch (TelegramApiException e) {
              e.printStackTrace();
          }
      }
  }

  public String getBotUsername() {
    return "Benebot 3.0";
  }

  public String getBotToken() {
      if (developerMode) {
          return "529232672:AAHxsYaI8WvGwbXs5jz4v1t2F4KIB1aTFoU";
      } else {
          return "675378494:AAFLUuKNX1ghyOeB6zURqmelsgvl-dQ0Xpo";
      }
  }
}
