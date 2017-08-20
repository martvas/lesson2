import java.sql.*;
import java.util.Scanner;

public class Main {
    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement preparedStatement;
    private static Scanner sc = new Scanner(System.in);
    private static ResultSet rSet;

    public static void main(String[] args) throws Exception {
        connect();
        statement = connection.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS Products\n" +
                "    (\n" +
                "    id     INTEGER PRIMARY KEY AUTOINCREMENT\n" +
                "                   UNIQUE\n" +
                "                   NOT NULL,\n" +
                "    prodid INTEGER UNIQUE NOT NULL,\n" +
                "    title  TEXT NOT NULL,\nD" +
                "    cost   INTEGER NOT NULL\n" +
                ");");
        statement.execute("DELETE FROM Products;");
        statement.execute("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='Products';");

        preparedStatement = connection.prepareStatement("INSERT INTO Products (prodid, title, cost)" +
                "VALUES (?, ?, ?)");

        connection.setAutoCommit(false);
        for (int i = 1; i <= 100000; i++) {
            preparedStatement.setInt(1, i);
            preparedStatement.setString(2, "product" + i);
            preparedStatement.setInt(3, i * 10);
            //Добавлю запрос в пакет, хотя необязательно так как БД  у нас на диске
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
        connection.setAutoCommit(true);

        introText();
        workWithDB();

        disconnect();
    }

    public static void workWithDB() {
        String query;
        do {
            System.out.println("Введите запрос: ");
            query = sc.nextLine();
            String[] split = query.split(" ");
            String queryType = split[0];
            try {
                if (queryType.equals("/price")) {
                    showProduct(split[1]);
                } else if (queryType.equals("/changeprice")) {
                    int newPrice = Integer.parseInt(split[2]);
                    changePrice(split[1], newPrice);
                } else if (queryType.equals("/byprice")) {
                    int minPrice = Integer.parseInt(split[1]);
                    int maxPrice = Integer.parseInt(split[2]);
                    productsByPrice(minPrice, maxPrice);
                } else if (queryType.equals("/exit")){
                    System.out.println("До свидания");
                } else System.out.println("Не правильный запрос. Попробуйте ещё раз.\n");
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Ваш запрос введен не корректно. \n");
            }
        } while (!(query.equals("/exit")));

    }

    public static void showProduct(String product) {
        try {
            rSet = statement.executeQuery("SELECT * FROM Products " +
                    "WHERE title = '" + product + "'");
            //Использовал метод rSet.isBeforeFirst(), так как когда использовал метод rSet.next()
            //курсор перепрыгивал на первую строку и не показывал в методе showDB() - строку из БД
            if (!rSet.isBeforeFirst()) {
                System.out.println("Такого товара нет\n");
            } else {
                showDB(rSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void changePrice(String product, int newPrice) {
        try {
            int resultUpdate = statement.executeUpdate("UPDATE Products SET cost = " + newPrice + " " +
                    "WHERE title = '" + product + "'");
            if (resultUpdate == 0) {
                System.out.println("Вы ввели не правильные данные. Данные в базе данных не изменились\n");
            } else {
                System.out.println("Вы изменили цену в товаре " + product + " на: " + newPrice);
                showProduct(product);
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void productsByPrice(int minPrice, int maxPrice) {
        try {
            rSet = statement.executeQuery("SELECT * FROM Products " +
                    "WHERE cost >= " + minPrice + " AND cost <=" + maxPrice + "");
            showDB(rSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void showDB(ResultSet rSet) throws SQLException {
        System.out.printf("%-5s %-12s %-15s %s\n", "ID", "Product ID", "Title", "Price");
        while (rSet.next()) {
            System.out.printf("%-5s %-12s %-15s %s\n",
                    rSet.getInt(1), rSet.getInt(2), rSet.getString(3), rSet.getInt(4));
        }
        System.out.println();
    }


    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:DBForProducts.db");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void introText() {
        System.out.println("1. Посмотреть цену продукта введите команду: \"/price\". Пример: \"/price product545\".");
        System.out.println("2. Изменить цену продукта введите команду: \"/changeprice\". Пример: \"/changeprice product10 10000\"");
        System.out.println("3. Посмотреть продукты в диапазоне цен: \"/byprice\". Пример: \"/byprice 100 600\"");
        System.out.println("Выйти - команда: \"/exit\" ");
        System.out.println();
    }
}
