import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class BotController extends TelegramLongPollingBot {
    private static boolean developerMode = true;
    private static ArrayList<Trigger> triggersInProcess = new ArrayList<Trigger>();
    private static ArrayList<Trigger> allTriggers = new ArrayList<Trigger>();

    public BotController() {
        // Initialize allTriggers
        try {
            allTriggers = Database.getTriggers();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Start a Timer task executing every day at 4 am to delete all unfinished Triggers
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Timer time1 = new Timer(); // Instantiate Timer Object
        Timer time2 = new Timer();

        // Start running the task at 04:00:00, period is set to 24 hours
        // if you want to run the task immediately, set the 2nd parameter to 0
        time1.schedule(new DeleteTask(), calendar.getTime(), TimeUnit.HOURS.toMillis(24));
        // Update our Trigger set every hour
        time2.schedule(new UpdateTask(), TimeUnit.HOURS.toMillis(1));
    }

    public void onUpdateReceived(Update update) {
        // Check whether we are receiving a group message or a private chat message
        if (update.getMessage().isGroupMessage() || update.getMessage().isSuperGroupMessage()) {
            if (update.hasMessage() && update.getMessage().hasText()) {

                // TODO Developer specific commands?

                for (Trigger trigger : allTriggers) {
                    String updateText = update.getMessage().getText().toLowerCase();
                    String triggerText = trigger.getCommand().toLowerCase();
                    if (updateText.contains(triggerText)) {
                        float probability = trigger.getProbability();
                        if (probability >= 1.0f) {
                            if (updateText.equals(triggerText)) {
                                sendReplyMessage(update.getMessage().getChatId().toString(),
                                        update.getMessage().getMessageId(),
                                        trigger.getContent());
                            }
                        } else if (isProbabilityHit(probability)) {
                            sendReplyMessage(update.getMessage().getChatId().toString(),
                                    update.getMessage().getMessageId(),
                                    trigger.getContent());
                        }
                        break;
                    }
                }
            }
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
                        if (currentCommand.getCommand().equals("/stop")) {

                        } else if (currentCommand.getCommand().equals("addtrigger")) {
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

    private void listAllCommands(int UserID) {
        Trigger[] triggers = null;
        String response = null;
        try {
            triggers= Database.getTriggersByOwnerID(UserID);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (triggers != null) {
            sendMessage(String.valueOf(UserID), "<b>Deine Trigger:</b>");
            for (Trigger trigger : triggers) {
                sendMessage(String.valueOf(UserID),
                        "- <b>Triggerwort:</b> <i>" + trigger.getCommand() +
                        "</i>\n- <b>Wahrscheinlichkeit:</b> <i>" + trigger.getProbability() +
                        "</i>\n- <b>Copypasta:</b> " + trigger.getContent());
            }
        } else {
            sendMessage(String.valueOf(UserID), "<b>Du hast leider noch keine Trigger hinzugefügt.</b>");
        }
    }

    private void handleAddTrigger(int state, Update update) {
        if (state == 0) {
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
                Trigger[] triggers = Database.getInProcessTriggers();
                // TODO Rewrite this piece of crap, new sql query etc
                int CID = -1;
                for (Trigger t : triggers) {
                    if (t.getCommand().equals(response)) {
                        CID = t.getCID();
                    }
                }
                if (CID > 0) {
                    triggersInProcess.add(new Trigger(CID, response, "", 0.0f, update.getMessage().getFrom().getId()));
                }
            } catch (SQLException s) {
                // TODO href mit passender id versehen vom eigentlichen Besitzer des Triggers
                sendMessage(update.getMessage().getFrom().getId().toString(),
                        "<b> Fehler beim hinzufügen des Triggers </b>\n" +
                                "<i> - </i> <a href=\"tg://user?id=" + update.getMessage().getFrom().getId()+
                                "\">Dieser junge Mann</a><i> hat ihn schon hinzugefügt</i>");
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
                            desiredTrigger.getCID()
                    );
                    success = true;
                    // Update the obj in our list
                    desiredTrigger.setContent(response);
                } else {
                    sendMessage(update.getMessage().getFrom().getId().toString(),
                            "<b>Sorry die Eingabe ist zu lange her</b>\n" +
                                    "<i> - versuch es neu mit </i>/addtrigger");
                }
            } catch (SQLException e) {
                sendMessage(update.getMessage().getFrom().getId().toString(),
                        "<b> Das sollte eigentlich nicht passieren. </b>" +
                                "\n<i> - Schick mir am besten einen Screenshot per pn.</i>");
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
                            "<b>Junge</b> ich hab doch gesagt mit <b>Komma</b> und <b>nicht</b> Punkt?!\n");
                    // Swapping our , for .
                    response = response.replace(',', '.');
                }
                try {
                    probability = Float.parseFloat(response);
                    // Update our Trigger in our database with our desired probability
                    Database.updateTriggerProbabilityByCID(
                            probability,
                            desiredTrigger.getCID()
                    );
                    // Update the obj in our list
                    desiredTrigger.setProbability(probability);
                    // Feedback to the user with the added trigger
                    sendMessage(update.getMessage().getChatId().toString(),
                            "<b>Dein Trigger wurde erfolgreich hinzugefügt</b>" +
                                    "\n<i> -</i> <b>Triggerwort: </b><i>" + desiredTrigger.getCommand() +
                                    "\n - </i><b>Wahrscheinlichkeit: </b><i>" + desiredTrigger.getProbability() +
                                    "\n - </i><b>Copypasta: </b><i> " + desiredTrigger.getContent() + "</i>");
                    // Set FINISHED to 1 indicating that the command is fully added
                    Database.updateTriggerFinishedByCID(1, desiredTrigger.getCID());
                    // Delete trigger from triggersInProcess and add it to allTriggers as it is finished
                    allTriggers.add(desiredTrigger);
                    triggersInProcess.remove(desiredTrigger);
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
            } else {
                sendMessage(update.getMessage().getFrom().getId().toString(),
                        "<b>Sorry die Eingabe ist zu lange her und nicht mehr im Speicher</b>\n" +
                                "<i> - versuch es neu mit /addtrigger</i>");            }
        }
    }

    private boolean sendReplyMessage(String ChatID, int MessageID, String message) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(ChatID)
                .setReplyToMessageId(MessageID)
                .setParseMode("HTML")
                .setText(message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return true;
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
            if (update.getMessage().getText().equals("/list")) {
                listAllCommands(update.getMessage().getFrom().getId());
            } else if (update.getMessage().getText().equals("/delete")) {
                sendMessage(update.getMessage().getFrom().getId().toString(),
                        "<b>Falscher Syntax!</b>\n" +
                                "<i> - /delete test schreiben um den Trigger 'test' zu löschen</i>");
            } else if (update.getMessage().getText().startsWith("/delete")) {
                String triggerToDelete = update.getMessage().getText().substring(7);
                if (triggerToDelete.charAt(0) == 32) {
                    triggerToDelete = triggerToDelete.substring(1);
                }
                try {
                    Trigger[] triggers = Database.getTriggersByOwnerID(update.getMessage().getFrom().getId());
                    boolean success = false;
                    for (Trigger trigger : triggers) {
                        if (trigger.getCommand().equals(triggerToDelete)) {
                            Database.deleteTrigger(triggerToDelete);
                            if (!allTriggers.remove(trigger)) {
                                triggersInProcess.remove(trigger);
                            }
                            success = true;
                            break;
                        }
                    }

                    if (success) {
                        sendMessage(update.getMessage().getFrom().getId().toString(),
                                "<b>Der Trigger '" + triggerToDelete + "' wurde erfolgreich gelöscht.</b>");
                    } else {
                        sendMessage(update.getMessage().getFrom().getId().toString(),
                                "<b>Fehler beim löschen des Triggers: " + triggerToDelete + "</b>\n" +
                                        "<i> - entweder du bist nicht der Urheber\n" +
                                        " - oder der Trigger konnte nicht gefunden werden</i>");
                    }
                } catch (SQLException e) {
                    sendMessage(update.getMessage().getFrom().getId().toString(),
                            "<b>Fehler beim löschen des Triggers: " + triggerToDelete + "</b>\n" +
                                    "<i> - schick mir diese Fehlermeldung bitte als screenshot</i>");
                    e.printStackTrace();
                }
            } else if (update.getMessage().getText().equals("/changelog")) {
                try {
                    String[] changelog = Database.getChangelog();
                    for (String text : changelog) {
                        sendMessage(update.getMessage().getFrom().getId().toString(), text);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            try {
                // Try adding our command as a new entry to the database
                Database.addCommandQueue(
                        update.getMessage().getFrom().getId(),
                        update.getMessage().getChatId(),
                        update.getMessage().getText(),
                        0);
            } catch (SQLException e) {
                // Since our entry already exists we need to decide whether our command just got called again or
                // if a different command was called
                try {
                    CommandQueue commandQueue = Database.getCommandQueueByUID(update.getMessage().getFrom().getId());
                    if (!commandQueue.getCommand().equals(update.getMessage().getText())) {
                        // If our saved command is not the same as the the one our user has triggered
                        // we have to update the COMMAND entry and set the State to 0
                        Database.updateCommandQueueCommandByIDs(
                                update.getMessage().getFrom().getId(),
                                update.getMessage().getChatId(),
                                update.getMessage().getText().substring(1)
                        );
                        Database.updateCommandQueueStateByIDs(
                                update.getMessage().getFrom().getId(),
                                update.getMessage().getChatId(),
                                0
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

    private boolean isProbabilityHit(float probability) {
        probability = probability * 100;
        Random rand = new Random();
        int randomNumber = 1 + rand.nextInt(100);
        if (randomNumber <= probability) {
            return true;
        } else {
            return false;
        }
    }

    public static void resetTriggersInProcess() {
        triggersInProcess = new ArrayList<Trigger>();
    }

    public static void updateTriggers() {
        try {
            allTriggers = Database.getTriggers();
        } catch (SQLException e) {
            e.printStackTrace();
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

    // TODO Write method for printing statements in log with current time
    // TODO Add this method in every try catch block
}
