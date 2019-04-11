import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;

public class BotController extends TelegramLongPollingBot {

  private static boolean developerMode = false;

  public void onUpdateReceived(Update update) {
      // Check whether we are receiving a group message or a private chat message
      if (update.getMessage().isGroupMessage() || update.getMessage().isSuperGroupMessage()) {
          // TODO Groupchat functionality
      } else {
          if (update.hasMessage()) {
              if (update.getMessage().hasText()) {
                  addNewUsersToDatabase(update);
                  handleCommandInit(update);

                  // Start to handle non-command messages and if they are part of command answer chain
                  CommandQueue currentCommand = null;
                  try {
                      currentCommand = Database.getCommandQueueByUID(update.getMessage().getFrom().getId());
                  } catch (SQLException e) {
                      e.printStackTrace();
                  }
                  // If the mysql query succeeded and we got our CommandQueue we only want to continue if it's active (>0)
                  if (currentCommand != null) {
                      // Handle different commands
                      if (currentCommand.getCommand().equals("addtrigger")) {
                          handleAddTrigger(currentCommand.getState(), update);
                      }

                  }
              } else if (update.getMessage().hasPhoto()) {
                  // TODO Photo functionality
              } else if (update.getMessage().hasVideoNote()) {
                  //  TODO VideoNote functionality
              } else if (update.getMessage().hasVideo()) {
                  //  TODO Video functionality
              } else if (update.getMessage().hasLocation()) {
                  //  TODO Location functionality
              }
          }
      }
      // TODO Add Inline Buttons
  }

  private void handleAddTrigger(int state, Update update) {
      if (state == 0) {
          // TODO darum kümmern dass nicht jedes mal solange state == 0 ist der letzte command geschickt wird
          // Initial state
          SendMessage sendMessage = new SendMessage()
                  .setChatId(update.getMessage().getChatId())
                  .setParseMode("HTML")
                  .setText("Welches wort soll die copypasta triggern?\n- Groß und Kleinschreibung beachten");
          try {
              execute(sendMessage);
          } catch (TelegramApiException e) {
              e.printStackTrace();
          } finally {
              try {
                  Database.updateCommandQueueStateByIDs(update.getMessage().getFrom().getId()
                          , update.getMessage().getChatId(), 1);
              } catch (SQLException e) {
                  e.printStackTrace();
              }
          }
      } else if (state == 1) {
          // The previous user response holds our desired command to update
          String response = update.getMessage().getText();
          boolean success = false;
          try {
              // Trying to add a new Trigger in the database
              success = Database.addTrigger(response, "", 0.0f, update.getMessage().getFrom().getId());
          } catch (SQLException e) {
              // TODO Feedback dass der command schon existiert
          }
          if (success) {
              // if we succeeded in adding a new Trigger our command chain can proceed
              try {
                  Database.updateCommandQueueStateByIDs(update.getMessage().getFrom().getId()
                          , update.getMessage().getChatId(), 2);
                  // Inform the user of the next step
                  SendMessage sendMessage = new SendMessage()
                          .setChatId(update.getMessage().getChatId())
                          .setText("Was soll die copypasta sein?");
                  execute(sendMessage);
              } catch (SQLException e) {
                  e.printStackTrace();
              } catch (TelegramApiException t) {
                  t.printStackTrace();
              }
          }
      } else if (state == 2) {
          // The previous user response holds our desired probability to update
          String response = update.getMessage().getText();
          boolean success = false;
          //try {
              // TODO Trying to modify our probability in the database
          //} catch (SQLException e) {
          //    e.printStackTrace();
          //}
          if (success) {
              // if we succeeded in updating our probablity we can advance in our command chain
              try {
                  Database.updateCommandQueueStateByIDs(update.getMessage().getFrom().getId()
                          , update.getMessage().getChatId(), 3);
                  // Inform the user of the next step
                  SendMessage sendMessage = new SendMessage()
                          .setChatId(update.getMessage().getChatId())
                          .setText("Response");
                  execute(sendMessage);
              } catch (SQLException e) {
                  e.printStackTrace();
              } catch (TelegramApiException t) {
                  t.printStackTrace();
              }
          }
      }
  }

  private void handleCommandInit(Update update) {
      if (update.getMessage().isCommand()) {
          if (update.getMessage().getText().equals("/addtrigger")) {
              boolean result = false;
              try {
                  // Try adding our command as a new entry to the database
                  result = Database.addCommandQueue(
                          update.getMessage().getFrom().getId(),
                          update.getMessage().getChatId(),
                          "addtrigger",
                          0);
              } catch (SQLException e) {
                  // Since our entry already exists we need to decide whether our command just got called again or
                  // if a different command was called
                  try {
                      CommandQueue commandQueue = Database.getCommandQueueByUID(update.getMessage().getFrom().getId());
                      if (!commandQueue.getCommand().equals("addtrigger")) {
                          // If our saved command is not the same as the the one our user has triggered
                          // we have to update the COMMAND entry
                          Database.alterCommandQueueCommandByIDs(
                                  update.getMessage().getFrom().getId(),
                                  update.getMessage().getChatId(),
                                  "addtrigger"
                          );
                      } else {
                          // Our saved command is the same so we just need to reset our command state
                          Database.updateCommandQueueStateByIDs(
                                  update.getMessage().getFrom().getId(),
                                  update.getMessage().getChatId(),
                                  0
                          );
                      }
                  } catch (SQLException e1) {
                      e1.printStackTrace();
                  }
              }
          } else if (update.getMessage().getText().equals("/start")) {
              try {
                  // If our user has pressed /start we want him to be added to our database
                  Database.addCommandQueue(
                          update.getMessage().getFrom().getId(),
                          update.getMessage().getChatId(),
                          "start",
                          0);
              } catch (SQLException e) {
                  e.printStackTrace();
              }
          }
      }
  }

  private void addNewUsersToDatabase(Update update) {
      // Add every user who interacts with the bot once to the user database
      if (update.getMessage().getText().equals("/start")) {
          int UserID = update.getMessage().getFrom().getId();
          // Grabbing the credentials
          String username = update.getMessage().getFrom().getUserName();
          String firstName = update.getMessage().getFrom().getFirstName();
          String lastName = update.getMessage().getFrom().getLastName();

          try {
              Database.addUser(UserID, username, firstName, lastName);
          } catch (SQLException e) {
              e.printStackTrace();
          }

          SendMessage sendMessage = new SendMessage()
                  .setChatId(update.getMessage().getChatId())
                  .setText("Bini beste Mann");
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
