package main;

import com.snappfood.database.DatabaseManager;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println(" اتصال به دیتابیس با موفقیت برقرار شد!");
        } catch (Exception e) {
            System.out.println(" خطا در اتصال:");
            e.printStackTrace();
        }
    }
}

