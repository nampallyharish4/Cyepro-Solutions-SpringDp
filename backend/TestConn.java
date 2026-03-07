import java.sql.*;
public class TestConn {
    public static void main(String[] args) {
        String url = args[0]; String user = args[1]; String pass = args[2];
        try {
            Connection conn = DriverManager.getConnection(url, user, pass);
            System.out.println("SUCCESS");
            conn.close();
        } catch (Exception e) {
            System.out.println("FAIL: " + e.getMessage());
        }
    }
}