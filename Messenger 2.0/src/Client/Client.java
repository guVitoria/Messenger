package Client;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

public class Client {
    private static BufferedReader reader; //Получает от севрера сообщение
    private static PrintWriter writer; //Отправляет сообщение на сервер
    private static String login; //Логин пользователя
    private static String password; //Пароль пользователя
    private static String userFound; //Для проверки пользователя при входе на наличие в базе данных
    private static AttributeSet mySet; //Сет для выделения текста, который получается от сервера
    private static AttributeSet mySet_2; //Сет для выделения текста, отправленных от одного пользователя лично ко второму
    private static AttributeSet mySet_3; //Сет для выделения текста, обычных сообщений
    private static DefaultListModel<ImageListElement> pic; //Модель для списка стикеров, доступных в чате
    private static ArrayList<String> allURL;
    private static ArrayList<String> allDis;

    private static JTextPane textArea; //Основное поле, куда выводятся сообщения в чате
    private static JTextField textField; //Поле для ввода сообщения
    private static DefaultListModel<String> usersOnline; //Модель для списка отнлайн пользователей
    private static StringBuilder buildSomething = new StringBuilder(); //Для подгрузки картинок в чат ввиде кусочка HTML файла


    public static void main(String[] args) {
        go();
    }

    private static void go() {
        userFound = "false";
        setNet();
        /**Создаем сокет для того, чтобы слушать сервер*/
        Thread thread = new Thread(new Listener());
        thread.start();


        Object[]  options_start = {"Вход", "Регистрация"};
        /**Стартовое окно, в котором пользователю предлагается выбор между входом или регистрацией*/
        int start = JOptionPane.showOptionDialog(null, "", "Добро пожаловать",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                createImg_2(createUrl("http://i31.beon.ru/76/49/2034976/71/86838871/x_d15807b8.png"), 250, 200),
                options_start, options_start[0]);


        if(start == JOptionPane.OK_OPTION){
            logToChat();
        } else if(start == JOptionPane.NO_OPTION){
            regToChat();
        } else {
            writer.println("@UserExit:"+login+ " " +password);
            writer.flush();
            System.exit(0);
        }
    }

    private static URL createUrl(String s) {
        URL url = null;
        try {
            url = new URL(s);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.out.println("Картинка не найдена(");
        }
        return url;
    }

    private static void logToChat(){

        JTextField log = new JTextField();
        log.setColumns(20);
        JPasswordField pas = new JPasswordField();
        pas.setColumns(20);
        Object[] fields_log = {"Введите логин: ", log, "Введите пароль:", pas};
        Object[]  options = {"Вход", "Отмена"};

        /**Выводим окошко входа, в котором запрашивается логин и пароль*/
        int ch = JOptionPane.showOptionDialog(null, fields_log, "Авторизация",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                createImg_2(createUrl("http://www.multday.ru/wp-content/uploads/2011/01/bob1.gif"), 200, 200),
                options, options[0]);

        if(ch == JOptionPane.OK_OPTION){
            //Производим вход
            login = log.getText();
            password = String.valueOf(pas.getPassword());
            writer.println("#UserFound:"+login+" "+password);
            writer.flush();
        } else{
            writer.println("@UserExit:"+login+ " " +password);
            writer.flush();
            System.exit(0);
        }
    }

    private static void regToChat(){

        JTextField log = new JTextField();
        log.setColumns(20);
        JPasswordField pas = new JPasswordField();
        pas.setColumns(20);
        Object[] fields_reg = {"Придумайте логин: ", log, "Придумайте пароль:", pas};
        Object[]  options_reg = {"Регистрация", "Отмена"};

        /**Выводим окошко входа, в котором запрашивается логин и пароль*/
        int reg = JOptionPane.showOptionDialog(null, fields_reg, "Регистрация",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                createImg_2(createUrl("http://1.bp.blogspot.com/-W45lXBWquAg/UnKoeIztqhI/AAAAAAAAET4/aVDrwSr4UxQ/s1600/png.png"), 200, 200),
                options_reg, options_reg[0]);

        if(reg == JOptionPane.OK_OPTION){
            login = log.getText();
            password = String.valueOf(pas.getPassword());
            writer.println("#UserReg:"+login+" "+password);
            writer.flush();
        } else{
            writer.println("@UserExit:"+login+ " " +password);
            writer.flush();
            System.exit(0);
        }

    }


    //Создание основного окна со всеми панелями
    public static void openMainWindow() {
        JFrame frame = new JFrame("Welcome to Sticker Chat ___ your login:" + login);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        JPanel panel = new JPanel();

        textArea = new JTextPane();
        textArea.setEditable(false);
        textArea.setContentType("text/html");


        mySet = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLUE);
        mySet_2 = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.MAGENTA);
        mySet_3 = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBounds(new Rectangle(0, 10, 350, 400));
        scrollPane.getViewport().add(textArea);

        textArea.setPreferredSize(new Dimension(500, 400));

        textField = new JTextField(33);
        JButton sendButton = new JButton("Отправить");
        sendButton.addActionListener(new Send());

        panel.add(scrollPane);
        panel.add(textField);
        panel.add(sendButton);

        /** Тут надо будет добавить список онлайн пользователей и стикеров
         * */

        //Список онлайн пользователей
        usersOnline = new DefaultListModel<String>();
        usersOnline.addElement("Общий чат");
        JPanel contents = new JPanel();
        contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
        JList<String> list2 = new JList<String>(usersOnline);
        JLabel label = new JLabel("<html><h3>Пользователи в сети");
        list2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane j_1 = new JScrollPane(list2);
        contents.add(label);
        contents.add(j_1);
        writer.println("#NeedLogins:"+login+" "+password);
        writer.flush();


        //Обработка нажатия на элемент списка онлайн пользователей
        list2.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Object element = list2.getSelectedValue();
                if(element != null){
                    if(element.equals("Общий чат")){
                        textField.setText("");
                    } else{
                        textField.setText("@for{" + element + "}: ");
                    }

                }   else textField.setText("");
            }
        });

        //Создание графического представления списка стикеров
        pic = new DefaultListModel<ImageListElement>();
        JList list = new JList(pic);
        list.setCellRenderer(new ImageListCellsRedender());
        JScrollPane j_2 = new JScrollPane(list);
        JLabel label_2 = new JLabel("<html><h3>Стикеры");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JButton uploadStick = new JButton("Добавить свой стикер");
        contents.add(label_2);
        contents.add(j_2);
        contents.add(uploadStick);
        allURL = new ArrayList<>();
        allDis = new ArrayList<>();
        writer.println("#NeedStickers:"+login+" "+password);
        writer.flush();


        uploadStick.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                //Сохраним выбранный пользователем файл в папку ресурсов проекта
                try {
                    //Получим описание и ссылку стикера от пользователя
                    JTextField StickDis = new JTextField();
                    JTextField StickUrl = new JTextField();
                    Object[] fields_nS = {"Введите описание стикера: ", StickDis, "Вставьте ссылку на стикер из интернета: ", StickUrl};
                    Object[]  options_nS = {"Загрузить!", "Отмена"};

                    int nS = JOptionPane.showOptionDialog(null, fields_nS, "Стикеры)))", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE, createImg_2(createUrl("http://ddu466.minsk.edu.by/ru/sm_full.aspx?guid=17283"), 200, 200), options_nS, options_nS[0]);

                    //Если пользователь ввел описание и ссылку, а не отменил добавление стикера
                    if(nS==JOptionPane.OK_OPTION){


                        if(createImg_2(createUrl(StickUrl.getText()), 20, 20) != null){
                            writer.println("#newStick:"+login+" "+StickUrl.getText() + " *" + StickDis.getText() + "*");
                            writer.flush();
                        }

                    }

                } catch (Exception ex) {
                    System.out.println("Ошибка при добавлении стикера!");

                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Ссылка на изображение некорректна!",
                            "Что-то тут не так...", 1);
                }
            }
        });

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Object element = list.getSelectedValue();
                if(element != null){
                    textField.setText(allDis.get(list.getSelectedIndex()));
                    list.clearSelection();
                }
            }
        });

        Box box = new Box(2);
        box.add(contents, 0);
        box.add(panel, 1);

        textArea.setText(null);

        frame.getContentPane().add(BorderLayout.CENTER, box);
        frame.setLocation(0, 0);
        frame.setSize(800, 480);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /**Обработка открытия и закрытия окна с программой*/
        frame.addWindowListener(new WindowAdapter() {

            /**При открытие отправляем серверу сообщение о том, что зашел пользователь
             * и создаем у этого пользователя список доступных стикеров*/
            @Override
            public void windowOpened(WindowEvent e) {
                writer.println("@UserEntre:"+login+ " " +password);
                writer.flush();
                //Создание списка стикеров
                // stickerListCreate();
            }

            /**При закрытии сообщаем серверу о том, что пользователь вышел
             * И удаляем пользователя из базы данных отлайн пользователей*/
            @Override
            public void windowClosing(WindowEvent e) {
                writer.println("@UserExit:"+login+ " " +password);
                writer.flush();
                System.exit(0);
            }
        });
    }

    /**Отправка сообщений серверу*/
    private static class Send implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String msg = login + ": " + textField.getText();
            writer.println(msg);
            writer.flush();
            textField.setText("");
            textField.requestFocus();
        }
    }


    private static Icon createImg_2(URL url, int widht, int height) {
        BufferedImage bufferedImageimg = null;
        try {
            bufferedImageimg = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Image dimg = bufferedImageimg.getScaledInstance(height, widht, Image.SCALE_SMOOTH);
        ImageIcon img = new ImageIcon(dimg);
        return img;
    }


    private static class Listener implements Runnable {

        @Override
        public void run() {
            String msg;
            try {
                /**Ждем пока данные полученных от сервера не будут пустыми*/
                while ((msg = reader.readLine()) != null) {
                    if(msg.contains("#UserFound:")){

                        StringBuffer buffer_1 = new StringBuffer(msg);
                        int x = buffer_1.indexOf(" ");
                        buffer_1.replace(0, x+1, "");
                        buffer_1.replace(0, buffer_1.indexOf(" ") + 1, "");
                        userFound = buffer_1.toString();

                        StringBuffer buffer_2 = new StringBuffer(msg);
                        buffer_2.replace(0, buffer_2.indexOf(" ")+1, "");
                        buffer_2.replace(buffer_2.indexOf(" "), buffer_2.length() , "");
                        System.out.println(buffer_1);
                        System.out.println(buffer_2);

                        if(userFound.equals("true") && buffer_2.toString().equals(login)) {
                            System.out.println("Меня нашли в базе данных!");
                            openMainWindow();
                        }
                        else if(buffer_2.toString().equals(login)) {
                            System.out.println("Меня не нашли в базе данных!" + userFound);
                            JOptionPane.showMessageDialog(null, "Не правильный логин или пароль",
                                    "Что-то тут не так...", 1);
                            logToChat();
                        }

                    } else if(msg.contains("@Server")){
                        addLine(msg);
                    }
                    else if(msg.startsWith("#UserEntre:")){
                        StringBuffer l = new StringBuffer(msg);
                        l.replace(0, l.indexOf(":")+1, "");
                        usersOnline.addElement(l.toString());
                    }
                    else if(msg.startsWith("#UserExit:")){
                        StringBuffer l = new StringBuffer(msg);
                        l.replace(0, l.indexOf(":")+1, "");
                        usersOnline.removeElement(l.toString());
                    }
                    else if(msg.contains("#UserReg:")){
                        StringBuffer buffer_1 = new StringBuffer(msg);
                        buffer_1.replace(0, buffer_1.indexOf("=") + 1, "");
                        System.out.println(buffer_1.toString());

                        StringBuffer buffer_2 = new StringBuffer(msg);
                        buffer_2.replace(0, buffer_2.indexOf(" ")+1, "");
                        buffer_2.replace(buffer_2.indexOf(" "), buffer_2.length(), "");

                        if(buffer_1.toString().equals("true") && buffer_2.toString().equals(login)){
                            openMainWindow();
                        } else if(buffer_2.toString().equals(login)){
                            JOptionPane.showMessageDialog(null, "Пользователь с таким именнем уже есть!",
                                    "Что-то тут не так...", 1);
                            regToChat();
                        }
                    }
                    else if(msg.startsWith("#NeedLogins:")){
                        String l = getMass(msg);
                        String log = getLogin(msg);
                        if(log.equals(login)){
                            String[] ptr = l.split(" ");
                            for (int i = 0; i < ptr.length; ++i){
                                usersOnline.addElement(ptr[i]);
                            }
                        }
                    }
                    else if(msg.startsWith("#NeedStickersUrl:")){
                        String l = getMass(msg);
                        String log = getLogin(msg);
                        if(log.equals(login)){
                            String[] ptr = l.split(" ");
                            for (int i = 0; i < ptr.length; ++i){
                                allURL.add(ptr[i]);
                                System.out.println(ptr[i]);
                            }
                        }
                    }
                    else if(msg.startsWith("#NeedStickersDis:")){
                        String l = getMass(msg);
                        String log = getLogin(msg);
                        if(log.equals(login)){
                            String[] ptr = l.split(" ");
                            for (int i = 0; i < ptr.length; ++i){
                                allDis.add(ptr[i]);
                                System.out.println(ptr[i]);
                            }
                        }
                    }
                    else if(msg.startsWith("#AllForImageSend:")){
                        StringBuffer log = new StringBuffer(msg);
                        log.replace(0, log.indexOf(":") + 1, "");
                        if(log.toString().equals(login)){
                            pic.clear();
                            pic.setSize(0);
                            for (int i = 0; i < allURL.size(); ++i) {
                                URL url = createUrl(allURL.get(i));
                                Icon img = createImg_2(url, 70, 70);
                                pic.add(i, new ImageListElement(img, allDis.get(i), url.toString()));
                            }
                        }
                    }
                    else if(msg.startsWith("#newStick:")){
                        StringBuffer url = new StringBuffer(msg);
                        url.replace(0, url.indexOf(":") + 1, "");
                        url.replace(url.indexOf(" "), url.length(), "");

                        StringBuffer dis = new StringBuffer(msg);
                        dis.replace(0, dis.indexOf(" ")+1, "");

                        allURL.add(url.toString());
                        allDis.add(dis.toString());
                        System.out.println(url.toString());
                        System.out.println(dis.toString());
                        pic.addElement(new ImageListElement(createImg_2(createUrl(url.toString()), 70, 70), dis.toString(), url.toString()));
                    }
                    //Сообщение для обработки личных сообщений
                    else if(msg.contains("@for{")){
                        StringBuffer buffer_1 = new StringBuffer(msg);
                        String buffer = new String(msg);
                        buffer_1.replace(buffer_1.indexOf(":"), msg.length()  ,  "");
                        buffer = buffer.substring(buffer.indexOf("{") + 1);
                        buffer = buffer.substring(0, buffer.indexOf("}"));

                        if(login.equals(buffer) || login.equals(buffer_1.toString())){
                            sendMsgWithStick(msg);
                        }
                    }
                    else{
                        sendMsgWithStick(msg);
                    }
                }
            } catch (Exception ex) {
                System.out.println("Ошибка при прослушке сервера!");
                ex.printStackTrace();
            }
        }

        private String getLogin(String msg) {
            StringBuffer l = new StringBuffer(msg);
            l.replace(0, l.indexOf(":") + 1, "");
            StringBuffer log = new StringBuffer(l);
            log.replace(log.indexOf("="), log.length(), "");
            return log.toString();
        }

        private String getMass(String msg) {
            StringBuffer l = new StringBuffer(msg);
            l.replace(0, l.indexOf(":") + 1, "");
            l.replace(0,l.indexOf("=") + 1, "");
            return l.toString();
        }
    }

    private static void addLine(String text) throws BadLocationException {
        if(text.contains("@Server")){
            textArea.getStyledDocument().insertString(textArea.getStyledDocument().getLength(), text + "\n", mySet);
        } else if(text.contains("@for{")){
            textArea.getStyledDocument().insertString(textArea.getStyledDocument().getLength(), text + "\n", mySet_2);
        } else if(text.contains("<img")){

            //doc создается для того, чтобы изображение обрабатывалось как html строка
            HTMLDocument doc = (HTMLDocument) textArea.getStyledDocument();
            try {
                buildSomething.setLength(0);
                buildSomething.append(text);
                doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), buildSomething.toString());
                textArea.setStyledDocument(doc);
                textArea.getStyledDocument().insertString(textArea.getStyledDocument().getLength(), "\n", mySet_3);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            textArea.getStyledDocument().insertString(textArea.getStyledDocument().getLength(), text + "\n", mySet_3);
        }
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    /**Настройка конекта с сервером*/
    private static void setNet(){
        try {
            /**Тут надо ввести ip и порт сервера*/
            Socket socket = new Socket("localhost", 3000);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
            System.out.println("К хосту подключился");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**Класс для создания объектов списка стикеров*/
    public static class ImageListElement {

        public ImageListElement(Icon i, String s, String url) {
            icon  = i;
            text = s;
            this.url = url;
        }

        public Icon getIcon() {
            return icon;
        }

        public String getText() {
            return text;
        }

        public String getUrl(){
            return url;
        }

        private Icon icon;
        private String text;
        private String url;
    }

    /**Для графического создания элемента списка*/
    public static class ImageListCellsRedender extends javax.swing.DefaultListCellRenderer {

        public ImageListCellsRedender() {
        }
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof ImageListElement) {
                ImageListElement elem = (ImageListElement) value;
                Icon i = elem.icon;
                String s = elem.text;
                JLabel l = (JLabel) super.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
                l.setIcon(i);
                return l;
            }

            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }

    }

    /**Метод для отправки сообщения со стикерами или без*/
    private static void sendMsgWithStick(String msg){
        try {
            /**Проверим на наличие стикера в сообщении*/
            if(msg.contains("*")){
                String str = msg;
                int first = str.indexOf("*");
                StringBuffer buffer_1 = new StringBuffer(str);
                StringBuffer buffer_2 = new StringBuffer(str);
                buffer_2.replace(0, first, "");
                int second = buffer_2.indexOf("*", 1);

                if(second != buffer_2.length()-1){
                    buffer_2.replace(second+1, buffer_2.length(), "");
                }
                String ur = null;

                for(int p = 0; p<pic.size(); p++){
                    if(buffer_2.toString().equals(pic.getElementAt(p).getText())){
                        ur = pic.getElementAt(p).getUrl();

                    }
                }
                first = str.indexOf("*", 1);
                second = str.indexOf("*", first+1);
                buffer_1.replace(first, second +1, "<img src='" + ur + "' width='150' height='150'/>");
                addLine(buffer_1.toString());
            }
            /**Иначе просто отправляем сообщение всем*/
            else {
                addLine(msg);
            }
        } catch(BadLocationException exc) {
            exc.printStackTrace();
        }
    }
}
