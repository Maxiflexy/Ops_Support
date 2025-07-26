package persistence;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


public class ConnectionUtil {

    final public static String LOCAL_DB_CONTEXT = "jdbc/LocalDB";

    final public static Connection getConnection() {

        Context initContext = null;
        Connection conn = null;
        Context envContext = null;
        DataSource ds = null;

        try {
            initContext = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
        }

        try {
            if (initContext != null) {
                envContext = (Context) initContext.lookup("java:/comp/env");
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }

        try {
            if (envContext != null) {

                ds = (DataSource) envContext.lookup(LOCAL_DB_CONTEXT);

            }
        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            if (ds != null) {

                conn = ds.getConnection();

            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return conn;
    }


    final public static Connection getConnection(DataSource ds) {

        Connection conn = null;

        try {
            if (ds != null) {

                conn = ds.getConnection();

            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return conn;
    }


    final public static void closeConnection(Connection conn) {

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        conn = null;

    }


}
