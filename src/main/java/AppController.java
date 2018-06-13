import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class AppController {
  public static void main(String[] args) {
    printLogo();
    ApiContextInitializer.init();
    TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
    try {
      telegramBotsApi.registerBot(new BotController());
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private static void printLogo () {
    System.out.print("__________                    ___.           __    __________.____     ________   \n"
        + "\\______   \\ ____   ____   ____\\_ |__   _____/  |_  \\______   \\    |    \\______ \\  \n"
        + " |    |  _// __ \\ /    \\_/ __ \\| __ \\ /  _ \\   __\\  |       _/    |     |    |  \\ \n"
        + " |    |   \\  ___/|   |  \\  ___/| \\_\\ (  <_> )  |    |    |   \\    |___  |    `   \\\n"
        + " |______  /\\___  >___|  /\\___  >___  /\\____/|__|    |____|_  /_______ \\/_______  /\n"
        + "        \\/     \\/     \\/     \\/    \\/                      \\/        \\/        \\/ ");
  }
}
