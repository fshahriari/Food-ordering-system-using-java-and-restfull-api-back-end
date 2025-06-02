package main;

import com.snappfood.database.DatabaseManager;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("successful connection");
        } catch (Exception e) {
            System.out.println("failed connection");
            e.printStackTrace();
        }
    }
}

