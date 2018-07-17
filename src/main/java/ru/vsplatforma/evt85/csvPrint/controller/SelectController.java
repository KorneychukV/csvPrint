package ru.vsplatforma.evt85.csvPrint.controller;

import com.jcraft.jsch.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class SelectController {

    Session session = null;
    String fullName = null;

    private Service<Void> createSessionThread;

    @FXML
    private ComboBox<String> selYear;
    @FXML
    private ComboBox<String> selMonth;
    @FXML
    private ComboBox<String> selDay;

    @FXML
    private VBox contentBox;

    @FXML
    private VBox root;

    @FXML
    public void initialize() {

        //текущая дата
        SimpleDateFormat formatYear = new SimpleDateFormat("yyyy");
        SimpleDateFormat formatMonth = new SimpleDateFormat("M");
        SimpleDateFormat formatDay = new SimpleDateFormat("dd");
        Date curDate = new Date();
        Integer year = Integer.valueOf(formatYear.format(curDate));
        Integer month = Integer.valueOf(formatMonth.format(curDate));
        Integer day = Integer.valueOf(formatDay.format(curDate));

        //заполнение списков и выбор текущей даты
        selYear.getItems().addAll("За все года");
        for (int i = year; i >= 2000; i--) {selYear.getItems().add(String.valueOf(i));}
        for (int i = year; i >= 2000; i--) {selYear.getItems().add(String.valueOf(i));}
        selYear.getSelectionModel().select(String.valueOf(year));
        selMonth.getItems().addAll("За все месяцы", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь");
        selMonth.getSelectionModel().select(month);
        selDay.getItems().add("За все дни");
        for (int i = 1; i<=31; i++) selDay.getItems().add(String.valueOf(i));
        selDay.getSelectionModel().select(day);


        Dialog<Void> dialog = new Dialog<Void>();
        dialog.setTitle("Подождите...");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
        dialog.show();

        if (createSession("root", "10.0.6.10", 22, null, null)) {
            showFiles(filteringDate());
            dialog.close();
        } else {
            dialog.close();
            session.disconnect();
            Stage stage = (Stage) root.getScene().getWindow();
            stage.close();
        }
    }



    @FXML
    public void yearUpdate(){
        showFiles(filteringDate());
    }

    @FXML
    public void monthUpdate(){
        showFiles(filteringDate());
    }

    @FXML
    public void dayUpdate(){
        showFiles(filteringDate());
    }

    public void selectFile(String fileName){
        //скачать файл
        System.out.println(fileName);
        try {
            fullName = "./archive/"+fileName.replace(".csv","");
            File dir = new File(fullName);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            copyRemoteToLocal("/mnt/ufs/media/mmcblk0p1/csv/", fullName, fileName);
            fullName += "/" + fileName;
        } catch (JSchException | IOException e) {
            System.out.println("Ошибка при скачивании файла");
            e.printStackTrace();
        }
        session.disconnect();
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
    }

    public void showFiles(String filterName){
        try{
            //вывод списка файлов
            List<String> lines = new ArrayList<String>();
            try {
                String command = "cd /mnt/ufs/media/mmcblk0p1/csv/\nls -1\n";
                Channel channel = initChannel(command, session);
                InputStream in = channel.getInputStream();
                channel.connect();
                String dataFromChannel = getDataFromChannel(channel, in);
                lines.addAll(Arrays.asList(dataFromChannel.split("\n")));
                contentBox.getChildren().clear();
                contentBox.setSpacing(5);
                System.out.println(filterName);
                for (String l: lines){
                    Pattern p = Pattern.compile(filterName+".*");
                    if(l.endsWith(".csv") && p.matcher(l).matches()){
                        Button but = new Button(l);
                        contentBox.getChildren().add(but);
                        but.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                selectFile(but.getText());
                            }
                        });
                        but.prefWidthProperty().set(390);
                    }
                }
                channel.disconnect();
            } catch (Exception e) {
                System.out.println("Ошибка: " + e);
            }
        }catch(Exception ex){
            System.err.println("Ошибка: " + ex);
            ex.printStackTrace();
        }
    }

    public String filteringDate(){
        String filterName;
        if (selYear.getSelectionModel().getSelectedItem().equals("За все года")) filterName = "....";
        else {
            filterName = selYear.getSelectionModel().getSelectedItem();
        }

        if (selMonth.getSelectionModel().getSelectedItem().equals("За все месяцы")) filterName += "..";
        else {
            if (selMonth.getSelectionModel().getSelectedIndex() < 10) {
                filterName += "0" + selMonth.getSelectionModel().getSelectedIndex();
            } else {
                filterName += selMonth.getSelectionModel().getSelectedIndex();
            }
        }

        if (selDay.getSelectionModel().getSelectedItem().equals("За все дни")) filterName += "..";
        else {
            if (Integer.valueOf(selDay.getSelectionModel().getSelectedItem()) < 10){
                filterName += "0" + selDay.getSelectionModel().getSelectedItem();
            }
            else {
                filterName += selDay.getSelectionModel().getSelectedItem();
            }
        }
        return filterName;
    }

    //соединяемся
    public Boolean createSession(String user, String host, int port, String keyFilePath, String keyPassword){
        try {
            JSch jsch = new JSch();

            if (keyFilePath != null) {
                if (keyPassword != null) {
                    jsch.addIdentity(keyFilePath, keyPassword);
                } else {
                    jsch.addIdentity(keyFilePath);
                }
            }

            Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session = jsch.getSession(user, host, port);
            session.setPassword("");
            session.setConfig(config);
            session.connect(10000);
            return true;
        } catch (JSchException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка подключения");
            alert.setContentText("Не удалось установить соединение с СПК");
            alert.showAndWait();
            return false;
        }
    }

    private Channel initChannel(String commands, Session session) throws JSchException {
        Channel channel = session.openChannel("exec");
        ChannelExec channelExec = (ChannelExec) channel;
        channelExec.setCommand(commands);
        channelExec.setInputStream(null);
        channelExec.setErrStream(System.err);
        return channel;
    }

    private String getDataFromChannel(Channel channel, InputStream in)
            throws IOException {
        StringBuilder result = new StringBuilder();
        byte[] tmp = new byte[2048];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 2048);
                if (i < 0) {
                    break;
                }
                result.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                int exitStatus = channel.getExitStatus();
                System.out.println("exit-status: " + exitStatus);
                break;
            }
        }
        return result.toString();
    }

    public void copyRemoteToLocal(String from, String to, String fileName) throws JSchException, IOException {
        from = from + File.separator + fileName;
        String prefix = null;

        if (new File(to).isDirectory()) {
            prefix = to + File.separator;
        }

        // exec 'scp -f rfile' remotely
        String command = "scp -f " + from;
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        byte[] buf = new byte[1024];

        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        while (true) {
            int c = checkAck(in);
            if (c != 'C') {
                break;
            }

            // read '0644 '
            in.read(buf, 0, 5);

            long filesize = 0L;
            while (true) {
                if (in.read(buf, 0, 1) < 0) {
                    // error
                    break;
                }
                if (buf[0] == ' ') break;
                filesize = filesize * 10L + (long) (buf[0] - '0');
            }

            String file = null;
            for (int i = 0; ; i++) {
                in.read(buf, i, 1);
                if (buf[i] == (byte) 0x0a) {
                    file = new String(buf, 0, i);
                    break;
                }
            }

            System.out.println("file-size=" + filesize + ", file=" + file);

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            // read a content of lfile
            FileOutputStream fos = new FileOutputStream(prefix == null ? to : prefix + file);
            int foo;
            while (true) {
                if (buf.length < filesize) foo = buf.length;
                else foo = (int) filesize;
                foo = in.read(buf, 0, foo);
                if (foo < 0) {
                    // error
                    break;
                }
                fos.write(buf, 0, foo);
                filesize -= foo;
                if (filesize == 0L) break;
            }

            if (checkAck(in) != 0) {
                System.exit(0);
            }

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            try {
                if (fos != null) fos.close();
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }

        channel.disconnect();
        session.disconnect();
    }

    public static int checkAck(InputStream in) throws IOException {
        int b = in.read();

        if (b == 0) return b;
        if (b == -1) return b;

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }

}
