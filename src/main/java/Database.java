import java.sql.*;
import java.util.ArrayList;

public class Database {
    private static final String USERNAME = "dbuser";
    private static final String PASSWORD = "dbaccess";
    private static final String CONN_STRING = "jdbc:mysql://192.168.188.46/benebot";

    public static void main(String args[]) throws SQLException {
        System.out.println("<i>- </i><b>Triggerwort:</b> <i>schwanger</i>\n" +
                "<i>- </i><b>Wahrscheinlichkeit:</b> <i>0.5</i>\n" +
                "<i>- </i><b>Copypasta:</b> <i> Absoluter Traum ist Frau zu schwängern</i>".charAt(118));
    }

    public static String[] getChangelog() throws SQLException {
        String changelog[];
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery("" +
                    "SELECT " +
                    "    * " +
                    "FROM m_changelog ");
            rs.last();
            changelog = new String[rs.getRow()];
            rs.first(); int i = 1;
            changelog[0] = rs.getString(2);
            while (rs.next()) {
                changelog[i] = rs.getString(2);
                i++;
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return changelog;
    }

    public static boolean deleteAllTriggersInProcess() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        boolean rs = false;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.execute("DELETE FROM `m_trigger` WHERE `m_trigger`.`FINISHED` = 0;");

        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return rs;
    }

    public static boolean updateCommandQueueCommandByIDs(int UserID, long ChatID, String alteredCommand) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean rs;
        if (alteredCommand == null) {
            return false;
        }
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            String insert = "UPDATE `m_commandqueue` " +
                            "SET `COMMAND` = ? " +
                            "WHERE `m_commandqueue`.`UID` = ? " +
                            "AND `m_commandqueue`.`CID` = ?";
            stmt = conn.prepareStatement(insert);
            stmt.setString(1, alteredCommand);
            stmt.setInt(2, UserID);
            stmt.setLong(3, ChatID);
            rs = stmt.execute();

        } catch (SQLException e) {
            throw e;
        } catch (NullPointerException n) {
            throw n;
        } finally {
            // Closing all resources properly
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return rs;
    }

    public static CommandQueue getCommandQueueByUID(int UserID) throws SQLException {
        CommandQueue commandQueue;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery("" +
                    "SELECT " +
                    "    * " +
                    "FROM m_commandqueue " +
                    "WHERE UID = " + UserID + ";");
            rs.first();
            commandQueue = new CommandQueue(
                    rs.getInt(1),
                    rs.getLong(2),
                    rs.getString(3),
                    rs.getInt(4)
            );
        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return commandQueue;
    }

    public static Trigger[] getTriggersByOwnerID (int OwnerID) throws SQLException {
        Trigger[] triggers;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery("" +
                    "SELECT " +
                    "    * " +
                    "FROM m_trigger " +
                    "WHERE `m_trigger`.`OWNER` = " + OwnerID + " ");
            rs.last();
            triggers = new Trigger[rs.getRow()];
            rs.first();

            int i = 0;
            do {
                Trigger triggerObj = new Trigger(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getFloat(4),
                        rs.getInt(5)
                );
                triggers[i] = triggerObj;
                i++;
            } while (rs.next());
        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return triggers;
    }

    public static boolean updateTriggerFinishedByCID(int Finished, int CID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean rs = false;
        try {
            // Only continue if our trigger is set properly
            if (CID != -1) {
                // Connecting to the database and updating our Triggers content
                conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
                String insert = "UPDATE `m_trigger` " +
                        "SET `FINISHED` = ? " +
                        "WHERE `m_trigger`.`CID` = ?";
                stmt = conn.prepareStatement(insert);
                stmt.setInt(1, Finished);
                stmt.setInt(2, CID);
                rs = stmt.execute();
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return rs;

    }

    public static boolean updateTriggerProbabilityByCID(float probability, int CID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean rs = false;
        try {
            // Only continue if our trigger is set properly
            if (CID != -1) {
                // Connecting to the database and updating our Triggers content
                conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
                String insert = "UPDATE `m_trigger` " +
                                "SET `PROBABILITY` = ? " +
                                "WHERE `m_trigger`.`CID` = ? ";
                stmt = conn.prepareStatement(insert);
                stmt.setFloat(1, probability);
                stmt.setInt(2, CID);
                rs = stmt.execute();
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return rs;
    }

    public static boolean updateTriggerContentByCID(String copypasta, int CID) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean rs = false;
        try {
            // Only continue if our trigger is set properly
            if (CID != -1) {
                // Connecting to the database and updating our Triggers content
                conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
                String insert = "UPDATE `m_trigger` " +
                                "SET `CONTENT` = ? " +
                                "WHERE `m_trigger`.`CID` = ?";
                stmt = conn.prepareStatement(insert);
                stmt.setString(1, copypasta);
                stmt.setInt(2, CID);
                rs = stmt.execute();
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return rs;
    }

    public static boolean updateCommandQueueStateByIDs(int UserID, long ChatID, int alteredState) throws SQLException{
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean rs = false;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            String insert = "UPDATE `m_commandqueue` " +
                            "SET `STATE` = ? " +
                            "WHERE `m_commandqueue`.`UID` = ? " +
                            "AND `m_commandqueue`.`CID` = ?";
            stmt = conn.prepareStatement(insert);
            stmt.setInt(1, alteredState);
            stmt.setInt(2, UserID);
            stmt.setLong(3, ChatID);
            rs = stmt.execute();

        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return rs;
    }

    public static boolean addCommandQueue(int UserID, long ChatID, String command, int state) throws SQLException{
        Connection conn = null;
        Statement stmt = null;
        boolean rs = false;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.execute("" +
                    "INSERT INTO " +
                    "   `m_commandqueue` (`UID`, `CID`, `COMMAND`, `STATE`) " +
                    "VALUES " +
                    "   ('" + UserID + "', '" + ChatID + "', '" + command + "', '" + state + "');");
        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return rs;
    }

    public static boolean addUser(int UserID, String username, String firstName, String lastName) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        boolean rs = false;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.execute("INSERT INTO `m_users` (`UID`, `USERNAME`, `FIRSTNAME`, `LASTNAME`) " +
                    "VALUES ('" + UserID + "', '" + username + "', '" + firstName + "', '" + lastName + "')");

        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return rs;
    }

    public static boolean deleteTrigger(String command) throws SQLException{
        Connection conn = null;
        Statement stmt = null;
        boolean rs;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.execute("DELETE FROM `m_trigger` WHERE `m_trigger`.`COMMAND` = '" + command + "';");

        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return rs;
    }

    public static boolean addTrigger(String command, String content, float probability, int OwnerID) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.execute("INSERT INTO `m_trigger` (`CID`, `COMMAND`, `CONTENT`, `PROBABILITY`, `OWNER`) " +
                    "VALUES (NULL, '" + command.toLowerCase() + "', '" + content + "', '" + probability + "', '" + OwnerID + "');");

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            // Closing all resources properly
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return true;
    }

    public static Trigger[] getInProcessTriggers() throws SQLException {
        Trigger[] carray;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery("SELECT * FROM m_trigger WHERE FINISHED = 0");

            rs.last();
            carray = new Trigger[rs.getRow()];
            int loopIndex = 0;

            // Saving every row into a single command object and adding those to our carray
            rs.first();
            do {
                Trigger command = new Trigger(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getFloat(4),
                        rs.getInt(5)
                );
                carray[loopIndex] = command;
                loopIndex++;
            } while (rs.next());
        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return carray;
    }

    public static ArrayList<Trigger> getTriggers() throws SQLException {
        ArrayList<Trigger> triggerList = new ArrayList<Trigger>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery("SELECT * FROM m_trigger WHERE FINISHED = 1");

            // Saving every row into a single command object and adding those to our carray
            rs.first();
            do {
                Trigger command = new Trigger(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getFloat(4),
                        rs.getInt(5)
                );
                triggerList.add(command);
            } while (rs.next());
        } catch (SQLException e) {
            throw e;
        } finally {
            // Closing all resources properly
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return triggerList;
    }
}
