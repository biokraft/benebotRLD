import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.ArrayList;

public class BotController extends TelegramLongPollingBot {
    private static boolean developerMode = false;
    private ArrayList<Trigger> triggersInProcess = new ArrayList<Trigger>();

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
            boolean messageSent = sendMessage(update.getMessage().getChatId().toString(),
                    "<b>Welches wort soll die copypasta triggern?</b>\n<i>- Groß- und Kleinschreibung beachten</i>");
            if (messageSent) {
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

                // Adding our added trigger to our ArrayList for continuing in the next step
                // Making sure we also get our CID for later on
                Trigger[] triggers = Database.getTriggers();
                int CID = -1;
                for (Trigger t : triggers) {
                    if (t.getCommand().equals(response)) {
                        CID = t.getCID();
                    }
                }
                if (CID > 0) {
                    triggersInProcess.add(new Trigger(CID, response, "", 0.0f, update.getMessage().getFrom().getId()));
                }
            } catch (SQLException e) {
                // TODO Feedback dass der command schon existiert
            }
            if (success) {
                // if we succeeded in adding a new Trigger our command chain can proceed
                try {
                    Database.updateCommandQueueStateByIDs(update.getMessage().getFrom().getId()
                            , update.getMessage().getChatId(), 2);
                    // Inform the user of the next step
                    sendMessage(update.getMessage().getChatId().toString(),
                            "<b>Was soll die copypasta sein?</b>\n" +
                                    "<i> - einfach reinpasten oder weiterleiten</i>");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else if (state == 2) {
            // The previous user response holds our desired content to update
            String response = update.getMessage().getText();
            boolean success = false;
            try {
                // Getting the triggers from our user that are still in process
                ArrayList<Trigger> userTriggers = getTriggersInProcessByOwnerID(update.getMessage().getFrom().getId());

                // Get the most recent one if there are any
                if (userTriggers.size() > 0) {
                    Trigger desiredTrigger = userTriggers.get(userTriggers.size() - 1);

                    // Update our Trigger in our database with our desired content(var response)
                    Database.updateTriggerContentByCID(
                            response,
                            desiredTrigger.getCID(),
                            update.getMessage().getFrom().getId()
                    );
                    success = true;
                    // Update the obj in our list
                    desiredTrigger.setContent(response);
                } else {
                    // TODO Feedback dass der Trigger nicht mehr gefunden werden konnte weil die letzt eingabe zu lange her ist
                }
            } catch (SQLException e) {
                // TODO Feedback an user dass der content nicht upgedatet werden konnte
                e.printStackTrace();
            }
            if (success) {
                // if we succeeded in updating our content we can advance in our command chain
                try {
                    Database.updateCommandQueueStateByIDs(update.getMessage().getFrom().getId()
                            , update.getMessage().getChatId(), 3);
                    // Inform the user of the next step
                    sendMessage(update.getMessage().getChatId().toString(),
                            "<b>Mit welcher Chance soll die copypasta getriggert werden?</b>\n" +
                                    "<i> - Zahl zwischen [0.00, 1.00]\n" +
                                    " - Bitte Punkt (.) und kein Komma (,) verwenden</i>");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else if (state == 3) {
            float probability = -1.0f;
            // We need to get our current Trigger from inProcessTrigger
            ArrayList<Trigger> userTriggers = getTriggersInProcessByOwnerID(update.getMessage().getFrom().getId());

            // Get the most recent one if there are any
            if (userTriggers.size() > 0) {
                Trigger desiredTrigger = userTriggers.get(userTriggers.size() - 1);

                // The previous user response holds our desired probability to update
                String response = update.getMessage().getText();

                if (response.contains(",")) {
                    // Send feedback for using , instead of .
                    sendMessage(update.getMessage().getChatId().toString(),
                            "<b>Junge</b> ich hab doch gesagt mit <b>Komma</b> und <b>nicht</b> Punkt?!\n" +
                                    "Alles muss man selber machen. Ich hoffe für dich dass das der einzige Fehler war.");
                    // Swapping our , for .
                    response = response.replace(',', '.');
                }
                try {
                     probability = Float.parseFloat(response);
                    // Update our Trigger in our database with our desired probability
                    Database.updateTriggerProbabilityByCID(
                            probability,
                            desiredTrigger.getCID(),
                            update.getMessage().getFrom().getId()
                    );
                    // Update the obj in our list
                    desiredTrigger.setProbability(probability);
                    // Feedback to the user with the added trigger
                    sendMessage(update.getMessage().getChatId().toString(),
                            "<b>Dein Trigger wurde erfolgreich hinzugefügt</b>" +
                                    "\n<i> -</i> <b>Triggerwort: </b><i>" + desiredTrigger.getCommand() +
                                    "\n - </i><b>Wahrscheinlichkeit: </b><i>" + desiredTrigger.getProbability() +
                                    "\n - </i><b>Copypasta: </b><i> " + desiredTrigger.getContent() + "</i>");
                } catch (NumberFormatException e) {
                    // Feedback to the user that the Probability could not be added
                    sendMessage(update.getMessage().getChatId().toString(),
                            "<b>Meine Fresse. Anscheinend hast du es irgendwie geschafft etwas zu schicken" +
                                    " was nicht dem Format 0,2 oder 0.2 entspricht. Glückwunsch dafür!</b>\n" +
                                    "<i> - fang am besten von vorne an mit </i>/addtrigger");
                    // Make sure to delete our broken Trigger
                    triggersInProcess.remove(desiredTrigger);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                //TODO FINISHED Flag zu m_trigger hinzufuegen wenn der trigger erfolgreich geaddet wurde
                //TODO regelmäßig alles aus der datenbank löschen wo das FINISHED Flag nicht gesetzt ist

            } else {
                // TODO Feedback dass der Trigger nicht mehr gefunden werden konnte weil die letzt eingabe zu lange her ist
            }
        }
    }

    private boolean sendMessage(String ChatID, String message) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(ChatID)
                .setParseMode("HTML")
                .setText(message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void handleCommandInit(Update update) {
        if (update.getMessage().isCommand()) {
            if (update.getMessage().getText().equals("/addtrigger")) {
                try {
                    // Try adding our command as a new entry to the database
                    Database.addCommandQueue(
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
                            Database.updateCommandQueueCommandByIDs(
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
                System.out.println(update.getMessage().getFrom().getFirstName()
                        + "[" + update.getMessage().getFrom().getId() + "] is already part of the databse");
            }

            sendMessage(update.getMessage().getChatId().toString(),
                    "Bini beste Mann");
        }
    }

    private ArrayList<Trigger> getTriggersInProcessByOwnerID(int OwnerID) {
        ArrayList<Trigger> result = new ArrayList<Trigger>();
        for (Trigger t : triggersInProcess) {
            if (t.getOwner() == OwnerID) {
                result.add(t);
            }
        }
        return result;
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

    // TODO Write method for printing statements in log with current time
    // TODO Add this method in every try catch block
}
