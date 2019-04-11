import java.sql.*;

public class Database {
    private static final String USERNAME = "dbuser";
    private static final String PASSWORD = "dbaccess";
    private static final String CONN_STRING = "jdbc:mysql://192.168.188.46/benebot";

    public static void main(String args[]) throws SQLException {
        deleteTrigger("Test");
    }

    public static boolean alterCommandQueueCommandByIDs(int UserID, long ChatID, String alteredCommand) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        boolean rs = false;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.execute("" +
                    "UPDATE `m_commandqueue` " +
                    "SET `COMMAND` = '" + alteredCommand + "' " +
                    "WHERE `m_commandqueue`.`UID` = " + UserID + "" +
                    "   AND `m_commandqueue`.`CID` = " + ChatID + "");

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

    public static CommandQueue getCommandQueueByUID(int UserID) throws SQLException {
        CommandQueue commandQueue;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        int STATE;
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

    public static String getCIDbyCommand(String command) {
        // TODO Commands sollten eine ID haben anhand der man überprüfen kann ob der command schon in der datenbank ist
        // TODO eventuell command als zahl einspeichern mit extra tabelle für zahl->command
        return null;
    }

    public static boolean updateCommandContentByCID () {
        return false;
    }

    public static boolean updateCommandQueueStateByIDs(int UserID, long ChatID, int alteredState) throws SQLException{
        Connection conn = null;
        Statement stmt = null;
        boolean rs = false;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.execute("" +
                    "UPDATE `m_commandqueue` " +
                    "SET `STATE` = '" + alteredState + "' " +
                    "WHERE `m_commandqueue`.`UID` = " + UserID + "" +
                    "   AND `m_commandqueue`.`CID` = " + ChatID + "");

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
        boolean rs = false;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.execute("DELETE FROM `m_commands` WHERE `m_commands`.`COMMAND` = '" + command + "';");

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
            stmt.execute("INSERT INTO `m_commands` (`CID`, `COMMAND`, `CONTENT`, `PROBABILITY`, `OWNER`) " +
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

    public static Trigger[] getTriggers() throws SQLException {
        Trigger[] carray = null;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery("SELECT * FROM m_commands");

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
                        rs.getFloat(4)
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
}
