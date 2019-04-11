import java.sql.*;

public class Database {
    private static final String USERNAME = "dbuser";
    private static final String PASSWORD = "dbaccess";
    private static final String CONN_STRING = "jdbc:mysql://192.168.188.46/benebot";

    private Connection connection = null;

    public static void main(String args[]) throws SQLException {
        deleteTrigger("Test");
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
            e.printStackTrace();
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
        boolean rs = false;
        try {
            // Connecting to the database and selecting all commands
            conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.execute("INSERT INTO `m_commands` (`CID`, `COMMAND`, `CONTENT`, `PROBABILITY`, `OWNER`) " +
                    "VALUES (NULL, '" + command.toLowerCase() + "', '" + content + "', '" + probability + "', '" + OwnerID + "');");

        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
