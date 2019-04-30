import gui.ava.html.image.generator.HtmlImageGenerator;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
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
        //Calendar calendar = Calendar.getInstance();
        //calendar.set(Calendar.HOUR_OF_DAY, 4);
        //calendar.set(Calendar.MINUTE, 0);
        //calendar.set(Calendar.SECOND, 0);
        //calendar.set(Calendar.MILLISECOND, 0);

        Timer time1 = new Timer(); // Instantiate Timer Object
        Timer time2 = new Timer();

        // Start running the task at 04:00:00, period is set to 24 hours
        // if you want to run the task immediately, set the 2nd parameter to 0
        time1.schedule(new DeleteTask(), TimeUnit.HOURS.toMillis(1));
        // Update our Trigger set every hour
        time2.schedule(new UpdateTask(), TimeUnit.HOURS.toMillis(1));
    }

    public void onUpdateReceived(Update update) {
        // Check whether we are receiving a group message or a private chat message
        if (update.getMessage().isGroupMessage() || update.getMessage().isSuperGroupMessage()) {
            // Mandatory check if we received a message with text
            if (update.hasMessage() && update.getMessage().hasText()) {
                // listcp functionality
                if (update.getMessage().getText().startsWith("/listcp")) {
                    listAllCopyastaToChat(update.getMessage().getChatId().toString());
                }
                // TODO Developer specific commands?
                // Checking for triggerwords in our received message
                for (Trigger trigger : allTriggers) {
                    // Get our current triggers content/text and our messageText
                    String messageText = update.getMessage().getText().toLowerCase();
                    String triggerText = trigger.getCommand().toLowerCase();
                    if (messageText.contains(triggerText)) {
                        float triggerProbability = trigger.getProbability();
                        // If our probability is >= 1.0 it will be handled as a copypasta
                        // meaning we just send the copypasta without replyMarkup
                        if (triggerProbability >= 1.0f) {
                            if (messageText.equals(triggerText)) {
                                this.logLn(trigger.getCommand() + " was triggered successfully in " + update.getMessage().getChatId());
                                sendMessage(update.getMessage().getChatId().toString(),
                                        trigger.getContent());
                            }
                        // If our probability is > 1.0 we'll let the dice decide
                        } else if (isProbabilityHit(triggerProbability)) {
                            this.logLn(trigger.getCommand() + " was pasted successfully in " + update.getMessage().getChatId());
                            sendReplyMessage(update.getMessage().getChatId().toString(),
                                    update.getMessage().getMessageId(),
                                    trigger.getContent());
                        }
                        break;
                    }
                }
            }
        } else {
            // This scope only applies to private chats with this bot
            if (update.hasMessage()) {
                if (update.getMessage().hasText() && update.getMessage().getFrom().getId() != 332328111) {
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
                        this.logLn( update.getMessage().getText()+ " was issued by " + update.getMessage().getFrom().getFirstName() + " (" + update.getMessage().getFrom().getId() + ")");
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

    private void listAllTriggersToChat(String ChatID) {
        this.logLn("Listing all Triggers to " + ChatID + "...");
        Trigger[] triggers = null;
        try {
            triggers= Database.getTriggersByOwnerID(Integer.valueOf(ChatID));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (triggers != null) {
            sendMessage(String.valueOf(ChatID), "<b>Deine Trigger:</b>\n" +
                    "<i> - Bitte geduldig sein. Hab nicht so viel Rechenpower ok</i>");
            // Specify our html code to render
            String htmlHead = "" +
                    "<head>\n" +
                    "   <meta charset=\"utf-8mb4\">\n" +
                    "   <style>\n" +
                    "       td {\n" +
                    "           border-bottom:1px solid;\n" +
                    "           border-right:1px solid;\n" +
                    "           padding:8px;\n" +
                    "           font-family: 'Comic Sans MS', cursive, sans-serif\n" +
                    "       }\n" +
                    "       th {\n" +
                    "           border-bottom:1px solid;\n" +
                    "           border-right:1px solid;\n" +
                    "           padding:8px;\n" +
                    "           font-family: 'Comic Sans MS', cursive, sans-serif\n" +
                    "       }\n" +
                    "   </style>\n" +
                    "</head>\n" +
                    "<table width=\"700px\">\n" +
                    "   <tr>\n" +
                    "       <th align=\"left\" width=\"15%\">\n" +
                    "           <b>Triggerwort</b>\n" +
                    "       </th>\n" +
                    "       <th align=\"left\" width=\"5%\">\n" +
                    "           <b>Wahrscheinlichkeit</b>\n" +
                    "       </th>\n" +
                    "       <th align=\"left\" width=\"80%\">\n" +
                    "           <b>Copypasta</b>\n" +
                    "       </th>\n" +
                    "   </tr>\n";
            String html = htmlHead; boolean hasRest = true;
            for (int i = 0; i < triggers.length; i++) {
                hasRest = true;
                html += "   <tr>\n" +
                        "       <td width=\"10%\">" + triggers[i].getCommand() + "</td>\n" +
                        "       <td width=\"5%\">" + triggers[i].getProbability() + "</td>\n" +
                        "       <td width=\"85%\">" + triggers[i].getContent() + "</td>\n" +
                        "   </tr>\n";
                if (i != 0 && (i+1) % 5 == 0) {
                    html += "</table>";
                    sendHtmlToPhoto(ChatID, html);
                    html = htmlHead;
                    hasRest = false;
                }
            }
            if (hasRest) {
                html += "</table>";
                // Send our remaining table to our user as a photo
                sendHtmlToPhoto(ChatID, html);
            }
            this.logLn("Triggers successfully listed to " + ChatID);
        } else {
            sendMessage(ChatID, "<b>Du hast leider noch keine Trigger hinzugefügt.</b>");
            this.logLn("Could not list any triggers for " + ChatID);
        }
    }

    private void listAllCopyastaToChat(String ChatID) {
        this.logLn("Listing all Copypasta to " + ChatID);
        sendMessage(ChatID, "<b>Jegliche verfügbare copypasta:</b>" +
                "\n<i> - Zu triggern mit copypasta xy</i>\n" +
                "<i> - Bitte geduldig sein. Hab nicht so viel Rechenpower ok</i>");
        // Specify our html code to render
        String htmlHead = "" +
                "<head>\n" +
                "   <meta charset=\"utf-8mb4\">\n" +
                "   <style>\n" +
                "       td {\n" +
                "           border-bottom:1px solid;\n" +
                "           border-right:1px solid;\n" +
                "           padding:8px;\n" +
                "           font-family: 'Comic Sans MS', cursive, sans-serif\n" +
                "       }\n" +
                "       th {\n" +
                "           border-bottom:1px solid;\n" +
                "           border-right:1px solid;\n" +
                "           padding:8px;\n" +
                "           font-family: 'Comic Sans MS', cursive, sans-serif\n" +
                "       }\n" +
                "   </style>\n" +
                "</head>\n" +
                "<table width=\"700px\">\n" +
                "   <tr>\n" +
                "       <th align=\"left\" width=\"10%\">\n" +
                "           <b>Triggerwort</b>\n" +
                "       </th>\n" +
                "       <th align=\"left\" width=\"85%\">\n" +
                "           <b>Copypasta</b>\n" +
                "       </th>\n" +
                "   </tr>\n";
        String html = htmlHead; int z = 0; boolean hasRest = true;
        for (int i = 0; i < allTriggers.size(); i++) {
            if (allTriggers.get(i).getProbability() >= 1.0f) {
                hasRest = true;
                html += "   <tr>\n" +
                        "       <td width=\"15%\">" + allTriggers.get(i).getCommand() + "</td>\n" +
                        "       <td width=\"85%\">" + allTriggers.get(i).getContent() + "</td>\n" +
                        "   </tr>\n";
                if (z != 0 && (z+1) % 5 == 0) {
                    html += "</table>";
                    sendHtmlToPhoto(ChatID, html);
                    html = htmlHead;
                    hasRest = false;
                }
                z++;
            }
        }
        if (hasRest) {
            html += "</table>";
            // Send our remaining table as photo
            sendHtmlToPhoto(ChatID, html);
        }
    }

    private void handleAddTrigger(int state, Update update) {
        if (state == 0) {
            // Initial state
            boolean messageSent = sendMessage(update.getMessage().getChatId().toString(),
                    "<b>Welches wort soll die copypasta triggern?</b>\n" +
                            "<i>- Groß- und Kleinschreibung ist egal</i>\n" +
                            "<i>- Bei Copypasta, sprich mit einer Wahrscheinlichkeit von 1 bitte vorne copypasta anfügen." +
                            " </i><b>Sprich:</b><i> copypasta xyz</i>");
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
            String response = update.getMessage().getText().toLowerCase();
            boolean success = false;
            try {
                // Trying to add a new Trigger in the database
                success = Database.addTrigger(response, "", 0.0f, update.getMessage().getFrom().getId());

                // Making sure we also get our CID for later on
                Trigger[] triggers = Database.getInProcessTriggersByUID(update.getMessage().getFrom().getId());
                int CID = -1;
                for (Trigger t : triggers) {
                    if (t.getCommand().equals(response)) {
                        CID = t.getCID();
                        break;
                    }
                }
                if (CID > 0) {
                    // Adding our added trigger to our ArrayList for continuing in the next step
                    triggersInProcess.add(new Trigger(CID, response, "", 0.0f, update.getMessage().getFrom().getId()));
                }
            } catch (SQLException s) {
                try {
                    int UserID = Database.getUserIDByTriggerword(response);
                    sendMessage(update.getMessage().getFrom().getId().toString(),
                            "<b>Fehler beim hinzufügen des Triggers</b>\n" +
                                    "<i>- </i> <a href=\"tg://user?id=" + UserID +
                                    "\">Dieser junge Mann</a> <i>hat ihn schon hinzugefügt</i>");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
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
                            "<b>Bitte Wahrscheinlichkeit für den Trigger angeben</b>\n" +
                                    "<i> - Zahl zwischen [0.00, 1.00]\n" +
                                    " - Bei 1.0 wird der Trigger nur ausgelöst wenn die Nachricht ledigleich aus dem" +
                                    " Triggerwort besteht und sonst nichts\n" +
                                    " - Unter 1.0 wird jedes mal wenn die Nachricht das Triggerwort enthält " +
                                    "mit der angegebenen Wahrscheinlichkeit die Copypasta ausgelöst</i>");
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
                    if (probability >= 1.0f
                            && !desiredTrigger.getCommand().startsWith("copypasta")
                            && !desiredTrigger.getCommand().startsWith("cp")) {
                        Database.updateTriggerCommandByCID("copypasta " + desiredTrigger.getCommand(),
                                desiredTrigger.getCID());
                        desiredTrigger.setCommand("copypasta " + desiredTrigger.getCommand());
                    }

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
                    // Log it
                    this.logLn(desiredTrigger.getCommand() + " was successfully added as a new Trigger by " +
                            update.getMessage().getFrom().getFirstName() + " (" + update.getMessage().getFrom().getId() + ")");
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
                    sendMessage(update.getMessage().getFrom().getId().toString(),
                            "<b> Das sollte eigentlich nicht passieren. </b>" +
                                    "\n<i> - Schick mir am besten einen Screenshot per pn.</i>");
                    e.printStackTrace();
                }
            } else {
                sendMessage(update.getMessage().getFrom().getId().toString(),
                        "<b>Sorry die Eingabe ist zu lange her und nicht mehr im Speicher</b>\n" +
                                "<i> - versuch es neu mit /addtrigger</i>");
            }
        }
    }

    private boolean sendReplyMessage(String ChatID, int MessageID, String message) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(ChatID)
                .setReplyToMessageId(MessageID)
                .setParseMode("HTML")
                .setText(message);
        try {
            Random random = new Random();
            int rand = random.nextInt((int) TimeUnit.SECONDS.toMillis(5));
            Thread.sleep(rand);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
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

    private void sendHtmlToPhoto(String ChatID, String html) {
        File photo = new File("list.png");
        // Start HtmlImageGenerator and save our respective file
        HtmlImageGenerator imageGenerator = new HtmlImageGenerator();
        imageGenerator.loadHtml(html);
        imageGenerator.saveAsImage(photo);

        // Send our generated table image to our user
        sendPhoto(String.valueOf(ChatID), photo);
    }

    private boolean sendPhoto(String ChatID, File photo) {
        SendPhoto sendMessage = new SendPhoto()
                .setChatId(ChatID)
                .setPhoto(photo);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void handleCommandInit(Update update) {
        if (update.getMessage().isCommand()) {
            // In this scope we reply to certain commands
            if (update.getMessage().getText().equals("/list")) {
                listAllTriggersToChat(update.getMessage().getFrom().getId().toString());
            }
            else if (update.getMessage().getText().equals("/listcp")) {
                listAllCopyastaToChat(update.getMessage().getFrom().getId().toString());
            }
            else if (update.getMessage().getText().equals("/delete")) {
                sendMessage(update.getMessage().getFrom().getId().toString(),
                        "<b>Falscher Syntax!</b>\n" +
                                "<i> - /delete test schreiben um den Trigger 'test' zu löschen</i>");
            }
            else if (update.getMessage().getText().startsWith("/delete")) {
                String triggerWordToDelete = update.getMessage().getText().substring(7);
                if (triggerWordToDelete.charAt(0) == 32) {
                    triggerWordToDelete = triggerWordToDelete.substring(1);
                }
                try {
                    Trigger[] triggers = Database.getTriggersByOwnerID(update.getMessage().getFrom().getId());
                    boolean success = false;
                    for (Trigger trigger : triggers) {
                        if (trigger.getCommand().equals(triggerWordToDelete)) {
                            Database.deleteTrigger(triggerWordToDelete);
                            removeTriggerByCID(trigger.getCID());
                            success = true;
                            break;
                        }
                    }

                    if (success) {
                        sendMessage(update.getMessage().getFrom().getId().toString(),
                                "<b>Der Trigger '" + triggerWordToDelete + "' wurde erfolgreich gelöscht.</b>");
                    } else {
                        sendMessage(update.getMessage().getFrom().getId().toString(),
                                "<b>Fehler beim löschen des Triggers: " + triggerWordToDelete + "</b>\n" +
                                        "<i> - entweder du bist nicht der Urheber\n" +
                                        " - oder der Trigger konnte nicht gefunden werden</i>");
                    }
                } catch (SQLException e) {
                    sendMessage(update.getMessage().getFrom().getId().toString(),
                            "<b>Fehler beim löschen des Triggers: " + triggerWordToDelete + "</b>\n" +
                                    "<i> - schick mir diese Fehlermeldung bitte als screenshot</i>");
                    e.printStackTrace();
                }
            }
            else if (update.getMessage().getText().equals("/changelog")) {
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
                this.logLn(firstName + " (" + UserID + ") "+ " started talking to Benebot");
            } catch (SQLException e) {
                System.out.println(update.getMessage().getFrom().getFirstName()
                        + "[" + update.getMessage().getFrom().getId() + "] is already part of the database");
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

    private void removeTriggerByCID(int CID) {
        // Remove our Trigger from the triggersInProcess list
        for (int i = 0; i < triggersInProcess.size(); i++) {
            Trigger t = triggersInProcess.get(i);
            if (t.getCID() == CID) {
                triggersInProcess.remove(t);
                // If we found our trigger to del we can return as this Trigger won't be saved in both lists
                return;
            }
        }
        // Remove our Trigger from the allTrigger list
        for (int i = allTriggers.size()-1; i >= 0; i--) {
            Trigger t = allTriggers.get(i);
            if (t.getCID() == CID) {
                allTriggers.remove(t);
                break;
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

    private void logLn(String logMessage) {
        String prefix = getDateWithTime();
        File file = new File("currentLog.txt");

        // If our logfile does not exist yet we need to create it
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Append our logMessage to our logLn
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("currentLog.txt", true));
            out.append(prefix + " " +  logMessage + "\n");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDateWithTime() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();

        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy']['HH:mm");

        String formattedDate = dateFormat.format(date);

        return "[LOG][" + formattedDate + "]";
    }
    // TODO Write method for printing statements in logLn with current time
    // TODO Add this method in every try catch block
    // TODO create new TimerTask for renaming or logLn.txt daily
}
