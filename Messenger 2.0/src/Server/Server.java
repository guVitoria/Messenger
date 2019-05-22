package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;

public class Server {
    private static ArrayList<PrintWriter> streams;
    private static ArrayList<String> logins; //Логины всех online пользователей
    private static ArrayList<String> imageUrls; //URL всех стикеров из бд
    private static ArrayList<String> imageDis; //Описание всех стикеров из бд
    private static Statement statement;
    private static Connection connection;
    private static PrintWriter writer;


    public static void main(String[] args) {
        go();
    }

    private static void go() {
        streams = new ArrayList<>();
        setDB();
        imageUrls = new ArrayList<>();
        imageDis = new ArrayList<>();
        logins = new ArrayList<>();
        loadAllAboutImages();
        System.out.println("CC = " + streams.size());
        try {
            /**Порт должен быть такой же как и в классы клиента в методе SetNet*/
            ServerSocket serverSocket = new ServerSocket(3000);

            while (true) {
                Socket socket = serverSocket.accept(); //Ждем подключения клиента
                System.out.println("New User!");
                writer = new PrintWriter(socket.getOutputStream());
                streams.add(writer);
                Thread thread = new Thread(new Listener(socket,  writer)) ;
                thread.start();
                System.out.println("CC = " + streams.size());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadAllAboutImages() {

        String SQL_img = "SELECT url, dis FROM `images`";
        ResultSet resultSet_img = null; //Для загрузки всех путей к стикерам
        try {
            resultSet_img = statement.executeQuery(SQL_img);

            while (resultSet_img.next()) {
                imageUrls.add(resultSet_img.getString("url"));
                imageDis.add(resultSet_img.getString("dis"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private static class Listener implements Runnable{

        BufferedReader reader;
        PrintWriter printWriter;

        Listener(Socket socket, PrintWriter printWriter){
            this.printWriter = printWriter;
            InputStreamReader inputStreamReader;
            try {
                inputStreamReader = new InputStreamReader(socket.getInputStream());
                reader = new BufferedReader(inputStreamReader);
            } catch ( Exception e) {
                System.out.println("Ошибка при чтении сокета сервером!");
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            String msg;

            try{
                while ((msg = reader.readLine()) != null){
                    //Сообщение для поиска пользователя в бд users
                    if(msg.contains("#UserFound")){

                        String login = getLogin(msg);
                        String pas = getPassword(msg);
                        tellEveryone("#UserFound: " + getLogin(msg)+ " " + foundInDataBase(login, pas));
                        System.out.println("#UserFound: "+login + " " + pas + " " + foundInDataBase(login, pas));
                    }
                    //Сообщение для оповещения о том, что пользователь успешно зашел
                    else if(msg.startsWith("@UserEntre:")){

                        //Добавим клиента в базу данных онлайн пользователей
                        String SQL = "INSERT INTO `usersOnline` (`login`) VALUES ( '"+getLogin(msg)+"');";
                        try {
                            statement.executeUpdate(SQL);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }

                        logins.add(getLogin(msg));
                        tellEveryone("@Server: "+ getLogin(msg) + " зашел/а в чат!");
                        tellEveryone("#UserEntre:"+getLogin(msg));
                        System.out.println("@Server: "+ getLogin(msg) + " зашел/а в чат!");
                    }
                    //Сообщение для регистрации
                    else if(msg.startsWith("#UserReg:")){
                        if(loginUnic(getLogin(msg))){
                            registerUser(getLogin(msg), getPassword(msg));
                            tellEveryone("#UserReg: "+ getLogin(msg) + " " + getPassword(msg) + "=true");
                            System.out.println("#UserReg: "+ getLogin(msg) + " " + getPassword(msg)+ "=true");
                        } else{
                            tellEveryone("#UserReg: "+ getLogin(msg) + " " + getPassword(msg) + "=false");
                            System.out.println("#UserReg: "+ getLogin(msg) + " " + getPassword(msg)+ "=false");
                        }
                    }
                    //Сообщение для обработки выхода пользователя
                    else if(msg.contains("@UserExit:")){
                        streams.remove(printWriter);
                        System.out.println("CC = " + streams.size());
                        //Уберем из базы данных онлайн пользователей
                        String SQL = "DELETE FROM `usersOnline` WHERE (`login` = '" + getLogin(msg) + "');";

                        try {
                            statement.executeUpdate(SQL);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        logins.remove(getLogin(msg));

                        goForAll("@Server: "+ getLogin(msg) + " вышел/а из чата!");
                        goForAll("#UserExit:"+getLogin(msg));
                        System.out.println("@Server: "+ getLogin(msg) + " вышел/а из чата!");

                    }
                    //Сообщение для отправки только что присоединившимуся пользователю всех пользователей в бд usersOnline
                    else if(msg.startsWith("#NeedLogins:")){
                        String allLogins = "";
                        System.out.println(logins.size());
                        for(int i = 0; i < logins.size(); ++i){
                            if(!getLogin(msg).equals(logins.get(i))){
                                allLogins += logins.get(i);
                                allLogins += " ";
                            }
                        }
                        tellEveryone("#NeedLogins:" + getLogin(msg) + "=" + allLogins);
                    }
                    //Сообщение для отправки только что присоединившимуся пользователю всех стикеров из бд images
                    else if(msg.startsWith("#NeedStickers:")){
                        String allUrls = "";
                        String allDis = "";

                        for(int i = 0; i < imageUrls.size(); ++i){
                            allUrls += imageUrls.get(i);
                            allUrls += " ";
                            allDis += imageDis.get(i);
                            allDis += " ";
                        }

                        tellEveryone("#NeedStickersUrl:" + getLogin(msg) + "=" + allUrls);
                        System.out.println("#NeedStickersUrl:" + getLogin(msg) + "=" + allUrls);
                        tellEveryone("#NeedStickersDis:" + getLogin(msg) + "=" + allDis);
                        tellEveryone("#AllForImageSend:" + getLogin(msg));
                    }
                    //Сообщение для обработки добавления стикера
                    else if(msg.startsWith("#newStick:")){
                        StringBuffer login = new StringBuffer(msg);
                        login.replace(0, login.indexOf(":") + 1, "");
                        login.replace(login.indexOf(" "), login.length(), "");

                        StringBuffer url = new StringBuffer(msg);
                        url.replace(0, url.indexOf(" ") + 1, "");
                        url.replace(url.indexOf(" "), url.length(), "");

                        StringBuffer dis = new StringBuffer(msg);
                        dis.replace(0, dis.indexOf(" ")+1, "");
                        dis.replace(0, dis.indexOf(" ")+1, "");
                        System.out.println(dis);

                        //Добавим новый стикер в нашу базу данных
                        String SQL = "INSERT INTO `images` (url, dis) VALUES ('"+url.toString()+"', '"+ dis.toString() +"')";
                        try {
                            statement.executeUpdate(SQL);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }

                        imageUrls.clear();
                        imageDis.clear();
                        loadAllAboutImages();

                        tellEveryone("#newStick:" + url + " " + dis);
                        tellEveryone("@Server: " + login + " добавил/а новый стикер!");
                        System.out.println("@Server: " + login + " добавил/а новый стикер!");

                    }
                    else{
                        System.out.println(msg);
                        tellEveryone(msg);
                    }
                }
            } catch (Exception ex){
                System.out.println("Ошибка при прослушке сервером клиента!");
                ex.printStackTrace();
            }
        }


    }

    //Отправка сообщенния всем пользователям
    private static void tellEveryone(String msg) {
        int x = msg.indexOf(":");
        String login = msg.substring(0, x);
        StringBuffer stringBuffer = new StringBuffer(msg);
        stringBuffer.replace(0, login.length() + 2  ,  "");
        goForAll(msg);
    }

    private static void goForAll(String msg) {
        java.util.Iterator<PrintWriter> it = streams.iterator();
        while (it.hasNext()){
            try {
                writer = it.next();
                writer.println(msg);
                writer.flush();
            } catch (Exception ex) {
                System.out.println("Ошибка при попытке пройти по чатам всех подключенных пользователей!");
                ex.printStackTrace();
            }
        }
    }

    //Подключение к бд
    private static void setDB() {
        String url = "jdbc:mysql://localhost:8889/Messenger?autoReconnect=true&useSSL=false";
        String login = "root";
        String password = "root";
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            connection = DriverManager.getConnection(url, login, password);
            statement = connection.createStatement();
            System.out.println("Подключился к бд");
        } catch (SQLException e) {
            System.out.println("Не удалось подключиться к базе данных!");
            e.printStackTrace();
        }
    }

    private static String getLogin(String msg){
        int x = msg.indexOf(":");
        StringBuffer ptr_msg = new StringBuffer(msg);
        ptr_msg.replace(0, x+1,  "");
        x = ptr_msg.indexOf(" ");
        StringBuffer login = new StringBuffer(ptr_msg);
        login.replace(x, login.length(), "");
        return login.toString();
    }
    private static String getPassword(String msg){
        int x = msg.indexOf(":");
        StringBuffer ptr_msg = new StringBuffer(msg);
        ptr_msg.replace(0, x,  "");
        x = ptr_msg.indexOf(" ");
        StringBuffer password = new StringBuffer(ptr_msg);
        password.replace(0, x+1,"");
        return password.toString();
    }

    /**Метод поиска такого пользователя в бд*/
    private static boolean foundInDataBase(String login, String password) {
        String SQL_login = "SELECT login FROM `users`";
        String SQL_password = "SELECT password FROM `users`";
        ArrayList logins = new ArrayList<String>();
        ArrayList passwords = new ArrayList<String>();

        try {
            ResultSet resultSet_login =  statement.executeQuery(SQL_login); //Для загрузки всех пользователей
            ResultSetMetaData metaData_login = resultSet_login.getMetaData();

            while (resultSet_login.next()){
                logins.add(resultSet_login.getString("login"));
            }
            resultSet_login.close();

            ResultSet resultSet_password =  statement.executeQuery(SQL_password); //Для загрузки паролей пользователей
            ResultSetMetaData metaData_msg = resultSet_password.getMetaData();
            while (resultSet_password.next()){
                passwords.add(resultSet_password.getString("password"));
            }
            resultSet_password.close();
            int i = 0;
            while (i < logins.size()){
                if(login.equals(logins.get(i)) && password.equals(passwords.get(i))){
                    return true;
                }
                i++;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**Метод для регистрации пользователя путем добавления в бд*/
    private static void registerUser(String login, String password) {
        String SQL = "INSERT INTO `users` (login, password) VALUES ('"+login+"', '"+password+"')";
        try {
            statement.executeUpdate(SQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**Проверка на уникальность такого логина, метод нужен для регистрации новых пользователей*/
    private static boolean loginUnic(String login) {
        String SQL_login = "SELECT login FROM `users`";
        ArrayList logins = new ArrayList<String>();

        try {
            ResultSet resultSet_login =  statement.executeQuery(SQL_login); //Для загрузки всех пользователей
            ResultSetMetaData metaData_login = resultSet_login.getMetaData();

            while (resultSet_login.next()){
                logins.add(resultSet_login.getString("login"));
            }
            resultSet_login.close();
            int i = 0;

            while (i < logins.size()){
                if(login.equals(logins.get(i))){
                    return false;
                }
                i++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}
