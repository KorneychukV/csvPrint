package ru.vsplatforma.evt85.csvPrint.controller;

import com.opencsv.CSVReader;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.util.Matrix;
import ru.vsplatforma.evt85.csvPrint.InputHeader;
import ru.vsplatforma.evt85.csvPrint.InputValues;
import ru.vsplatforma.evt85.csvPrint.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MainController {

    @FXML
    private BorderPane root;

    @FXML
    private TabPane tabPane;

    SingleSelectionModel<Tab> selectionModel;

    @FXML
    private CheckBox detailChk;
    @FXML
    private CheckBox camChk;
    @FXML
    private CheckBox dissChk;

    java.util.List<InputValues> inpVals = null;
    List<InputHeader> inpHead = null;
    File selectedCSV = null;
    Integer printFlag = 1;
    String startDate = null;
    String endDate = null;

    //инициализация графиков
    //графики с температурой
    XYChart.Series detSeries = new XYChart.Series();
    XYChart.Series camSeries = new XYChart.Series();
    XYChart.Series dissSeries = new XYChart.Series();
    //графики с давлением и расходом
    XYChart.Series pressSeries = new XYChart.Series();

    //график температуры
    final CategoryAxis xAxis = new CategoryAxis();
    final NumberAxis yAxis = new NumberAxis();
    final LineChart<String, Number> tempChart = new LineChart<String, Number>(xAxis, yAxis);
    //график давления и расхода
    final CategoryAxis x1Axis = new CategoryAxis();
    final NumberAxis y1Axis = new NumberAxis();
    final LineChart<String, Number> percChart = new LineChart<String, Number>(x1Axis, y1Axis);

    @FXML
    public void initialize() {
        //легенда
        detSeries.setName("Температура детали");
        camSeries.setName("Температура в камере");
        dissSeries.setName("Температура диссоциатора");
        pressSeries.setName("Давление в камере");
        //ось графика температуры
        xAxis.setLabel("Время");
        yAxis.setLabel("Температура, °C");
        //ось графика с процентами
        x1Axis.setLabel("Время");
        y1Axis.setLabel("Давление, мм.рт.ст");
        //фон приложения
        root.setBackground(new Background(new BackgroundFill(
                Color.WHITE,
                new CornerRadii(0),
                new Insets(0))));
        //
        selectionModel = tabPane.getSelectionModel();
        //выбор графика
        tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                if (selectionModel.isSelected(0)){
                    root.setCenter(tempChart);
                    printFlag = 1;
                }
                else {
                    root.setCenter(percChart);
                    printFlag = 2;
                }
                paintChart(tempChart, percChart);
            }
        });
        readChangeFile();
    }

    //открыть таблицу событий
    @FXML
    protected void openEventTable(){
        Stage modal = new Stage();
        FXMLLoader loader = new FXMLLoader();
        try {
            Parent table = (Parent) loader.load(getClass().getResourceAsStream("/fxml/eventTable.fxml"));
            Scene scene = new Scene(table);
            modal.setScene(scene);
            modal.setTitle("Таблица событий");
            TableController tab = loader.getController();
            tab.readCSV(selectedCSV.getPath(), selectedCSV.getName());
            modal.setWidth(1000);
            modal.setHeight(500);
            modal.setMaxWidth(2000);
            modal.setMaxHeight(2000);
            modal.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //открыть csv файл из файловой системы СПК и прочитать значения
    @FXML
    protected void openFile(){
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader loader = new FXMLLoader();
        try {
            Parent sec = (Parent) loader.load(getClass().getResourceAsStream("/fxml/selectFile.fxml"));
            Scene scene = new Scene(sec);
            modal.setScene(scene);
            modal.setTitle("Выберите файл");
            modal.resizableProperty().setValue(false);
            SelectController child = loader.getController();
            modal.showAndWait();
            System.out.println("Имя открываемого файла: " + child.fullName);
            if (child.fullName != null) {
                selectedCSV = new File(child.fullName);
                readCSV();
                paintChart(tempChart, percChart);
                selectionModel.select(0);
                printFlag = 1;
                root.setCenter(tempChart);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //открыть csv файл из файловой системы ПК и прочитать значения
    private File openCSV(){
        //Выбор файла
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter ef = new FileChooser.ExtensionFilter(
                "Comma-Separated Values",
                "*.csv");
        fileChooser.getExtensionFilters().add(ef);
        selectedCSV = fileChooser.showOpenDialog(null);
        //Чтение из файла
        readCSV();
        return selectedCSV;
    }

    //Чтение из файла
    public void readCSV(){
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(selectedCSV.getPath());
            InputStreamReader isr = new InputStreamReader(fis, "Windows-1251");
            CSVReader reader = new CSVReader(isr);
            String[] record = null;
            record = reader.readNext();
            inpVals = new ArrayList<InputValues>();
            inpHead = new ArrayList<InputHeader>();
            for (String r : record) {
                inpHead.add(new InputHeader(r));
            }
            while ((record = reader.readNext()) != null) {
                //if (record[2].equals("Состояние системы")) {
                //Date
                SimpleDateFormat formDate = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");


                if (!((record[4].equals("#NaN")) || (record[5].equals("#NaN")) || (record[6].equals("#NaN")) ||
                        (record[7].equals("#NaN"))))  {
                    InputValues inpVal = new InputValues(formDate.parse(record[0] + " " + record[1]), record[3],
                        Float.valueOf(record[4]), Float.valueOf(record[5]), Float.valueOf(record[6]),
                        Float.valueOf(record[7]));
                    inpVals.add(inpVal);
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла.\n" + e);
            JOptionPane.showMessageDialog(new JFrame(),
                    "Ошибка чтения файла",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    //печать текущего графика
    @FXML
    protected void printButAction(){
        printPDF(saveToPDF());
    }
    //печать обоих графиков
    @FXML
    protected void printTwiceButAction(){
        printFlag = 3;
        printPDF(saveToPDF());
        selectionModel.select(0);
        printFlag = 1;
    }
    //открытие техпроцесса
    @FXML
    protected void openTPAction(){
        openCSV();
        if (selectedCSV != null){
            paintChart(tempChart, percChart);
            selectionModel.select(0);
            root.setCenter(tempChart);
            printFlag = 1;
        }
    }
    //обновление при выборе графика
    @FXML
    protected void chkUpdate(){
        editChangeFile();
        if (selectedCSV != null){
            paintChart(tempChart, percChart);
        }
    }

    private void readChangeFile(){
        Properties prop = new Properties();
        File changeFile = new File("./settings.properties");
        if (changeFile.exists()){
            try {
                //Загружаем свойства из файла
                prop.load(new FileInputStream(changeFile));
                //Получаем в переменную значение конкретного свойства
                detailChk.setSelected(Boolean.parseBoolean(prop.getProperty("detailTemp")));
                camChk.setSelected(Boolean.parseBoolean(prop.getProperty("camTemp")));
                dissChk.setSelected(Boolean.parseBoolean(prop.getProperty("dissTemp")));

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void editChangeFile(){
        Properties prop = new Properties();
        File changeFile = new File("./settings.properties");
        if (!changeFile.exists()){
            try {
                changeFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //Устанавливаем значение свойста
            prop.setProperty("detailTemp", String.valueOf(detailChk.isSelected()));
            prop.setProperty("camTemp", String.valueOf(camChk.isSelected()));
            prop.setProperty("dissTemp", String.valueOf(dissChk.isSelected()));
            //Сохраняем свойства в файл.
            prop.store(new FileOutputStream(changeFile), null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //сохранение графика во временный png
    private void savePNG(WritableImage image, String nameFile, Boolean rotFlag){
        String pathToResultChart = selectedCSV.getParent() + nameFile;
        //поворот
        BufferedImage rotImg = SwingFXUtils.fromFXImage(image, null);
        BufferedImage newImg = null;
        if (rotFlag) {
            newImg = new BufferedImage(
                    rotImg.getHeight(),
                    rotImg.getWidth(),
                    rotImg.getType());
            Graphics2D g2 = newImg.createGraphics();
            g2.rotate(Math.toRadians(270),
                    (rotImg.getWidth()/2),
                    (rotImg.getWidth()/2));
            g2.drawImage(rotImg, null, 0, 0);
        } else {
            newImg = rotImg;
        }
        final File resultChart = new File(pathToResultChart);
        try {
            ImageIO.write(newImg, "png", resultChart);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения графика. \n" + e);
        }
    }
    //сохранение графика в PDF
    private PDDocument saveToPDF(){
        //сохранение графика(вывод в PNG c поворотом)
        Transform transform = Transform.scale(3,3);
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setTransform(transform);

        WritableImage image = null;
        WritableImage image1 = null;

        String pathToResultChart = selectedCSV.getParent() + "/chart.png";
        String pathToResultChart1 = selectedCSV.getParent() + "/chart1.png";
        if (printFlag == 1){
            image = tempChart.snapshot(snapshotParameters, null);
            savePNG(image, "/chart.png", true);
            return prepOnePDF(pathToResultChart, selectedCSV.getName());
        } else if(printFlag == 2){
            image1 = percChart.snapshot(snapshotParameters, null);
            savePNG(image1, "/chart1.png", true);
            return prepOnePDF(pathToResultChart1, selectedCSV.getName());
        } else {

            root.setCenter(percChart);
            paintChart(tempChart, percChart);
            image1 = percChart.snapshot(snapshotParameters, null);
            savePNG(image1, "/chart1.png", false);

            root.setCenter(tempChart);
            image = tempChart.snapshot(snapshotParameters, null);
            paintChart(tempChart, percChart);
            savePNG(image, "/chart.png", false);

            return prepTwoPDF(pathToResultChart, pathToResultChart1, selectedCSV.getName());
        }
    }
    //подготовка PDF с одним графиком
    private PDDocument prepOnePDF(String pathToResultChart, String docName){
        //вывод в PDF
        PDDocument pdfile = new PDDocument();
        try {
            String pathToPdf = selectedCSV.getPath().replaceFirst(".csv", ".pdf");
            pdfile.addPage(new PDPage());
            PDPage page = pdfile.getPage(0);
            page.setMediaBox(new PDRectangle().LEGAL.A4);
            page.setRotation(90);
            PDImageXObject pdImage = PDImageXObject.createFromFile(pathToResultChart, pdfile);
            PDPageContentStream contents = new PDPageContentStream(pdfile, page);
            pdImage.setInterpolate(true);

            contents.drawImage(pdImage, 17, 57, 770f / (Float.valueOf(pdImage.getHeight()) /
                    Float.valueOf(pdImage.getWidth())), 770);

            contents.setNonStrokingColor(java.awt.Color.BLACK);
            //Рамка
            contents.addRect(14, 57, 567, 771);
            contents.addRect(538,304, 43, 524);
            contents.addRect(538, 304, 43, 66);
            contents.addRect(538, 462, 43, 90);

            InputStream is = Main.class.getClassLoader().getResourceAsStream("ARIALUNI.TTF");
            contents.setFont(PDType0Font.load(pdfile, is), 12);

            contents.beginText();
            contents.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(90), 563, 309));
            //contents.showText("Оператор");
            contents.endText();

            contents.beginText();
            contents.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(90), 555, 630));
            String temp = docName.substring(6,8) + "." + docName.substring(4,6) + "." +
                    docName.substring(0,4) + " ";
            temp += docName.substring(9,11) + ":" + docName.substring(11,13) + ":" + docName.substring(13,15);
            contents.showText(temp);
            contents.endText();

            is = Main.class.getClassLoader().getResourceAsStream("ARIALUNI.TTF");
            contents.setFont(PDType0Font.load(pdfile, is), 10);
            contents.beginText();
            contents.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(90), 570, 595));
            contents.showText(startDate + " - " + endDate);
            contents.endText();

            contents.stroke();

            System.out.println("Image inserted");


            (new File(pathToResultChart)).delete();

            is.close();
            contents.close();

            pdfile.save(pathToPdf);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pdfile;
    }
    //подготовка PDF с двумя графиками
    private PDDocument prepTwoPDF(String pathToResultChart, String pathToResultChart1, String docName){
        //вывод в PDF
        PDDocument pdfile = new PDDocument();
        try {
            String pathToPdf = selectedCSV.getPath().replaceFirst(".csv", "_both.pdf");
            pdfile.addPage(new PDPage());
            PDPage page = pdfile.getPage(0);
            page.setMediaBox(new PDRectangle().LEGAL.A4);

            PDImageXObject pdImage = PDImageXObject.createFromFile(pathToResultChart, pdfile);
            PDImageXObject pdImage1 = PDImageXObject.createFromFile(pathToResultChart1, pdfile);

            PDPageContentStream contents = new PDPageContentStream(pdfile, page);
            pdImage.setInterpolate(true);
            pdImage1.setInterpolate(true);

            Float h = 520f * (Float.valueOf(pdImage.getHeight()) /
                    Float.valueOf(pdImage.getWidth()));
            contents.drawImage(pdImage, 60, 820-h-30, 520, h);
            contents.drawImage(pdImage1, 60, 820-2*h-60, 520, h);

            contents.setNonStrokingColor(java.awt.Color.BLACK);
            //Рамка

            contents.addRect(57, 14, 524, 814);
            contents.addRect(57,14, 524, 43);
            contents.addRect(57, 14, 66, 43);
            contents.addRect(246, 14, 90, 43);

            InputStream is = Main.class.getClassLoader().getResourceAsStream("ARIALUNI.TTF");
            contents.setFont(PDType0Font.load(pdfile, is), 12);
            is.close();
            contents.beginText();
            contents.newLineAtOffset(61,30);
            //contents.showText("Оператор");
            contents.endText();

            contents.beginText();
            contents.newLineAtOffset(400,40);
            String temp = docName.substring(6,8) + "." + docName.substring(4,6) + "." +
                    docName.substring(0,4) + " ";
            temp += docName.substring(9,11) + ":" + docName.substring(11,13) + ":" + docName.substring(13,15);
            contents.showText(temp);
            contents.endText();

            is = Main.class.getClassLoader().getResourceAsStream("ARIALUNI.TTF");
            contents.setFont(PDType0Font.load(pdfile, is), 10);
            is.close();

            contents.beginText();
            contents.newLineAtOffset(365,25);
            contents.showText(startDate + " - " + endDate);
            contents.endText();


            contents.stroke();

            (new File(pathToResultChart)).delete();
            (new File(pathToResultChart1)).delete();
            System.out.println("Image inserted");
            contents.close();
            pdfile.save(pathToPdf);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pdfile;
    }
    //перерисовка графика
    private void paintChart(LineChart tempChart, LineChart percChart){
        SimpleDateFormat formDate = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");
        SimpleDateFormat formDateSeries = new SimpleDateFormat("dd.MM HH:mm:ss");
        detSeries.getData().clear();
        camSeries.getData().clear();
        dissSeries.getData().clear();

        pressSeries.getData().clear();

        startDate = formDate.format(inpVals.get(0).getDate());
        endDate = formDate.format(inpVals.get(inpVals.size()-1).getDate());

        Double amount = Math.ceil((float)inpVals.size()/1000f);

        System.out.println(amount);

        for(int i = 0; i < inpVals.size(); i+=amount){
            //Температура детали
            //Long diff = inpVals.get(i).getDate().getTime() - inpVals.get(0).getDate().getTime();
            //long mins = diff / (60 * 1000) % 60;
            //long hours = diff / (60 * 60 * 1000) % 24;
            //long days = diff / (24 * 60 * 60 * 1000);
            //String date = (days * 24 + hours) + " час " +mins+ " мин";
            String date = formDateSeries.format(inpVals.get(i).getDate().getTime());
            if(detailChk.isSelected()) {detSeries.getData().add(new XYChart.Data(date, inpVals.get(i).getTempDetail()));}
            //Температура в камере
            if(camChk.isSelected()) {camSeries.getData().add(new XYChart.Data(date, inpVals.get(i).getTempCamera()));}
            //Температура диссоциатора
            if(dissChk.isSelected()) {dissSeries.getData().add(new XYChart.Data(date, inpVals.get(i).getTempDiss()));}
            //Давление в камере
            pressSeries.getData().add(new XYChart.Data(date, inpVals.get(i).getPressCamera()*10));
        }
        //Температура детали
//        Long diff = inpVals.get(inpVals.size()-1).getDate().getTime() - inpVals.get(0).getDate().getTime();
//        long mins = diff / (60 * 1000) % 60;
//        long hours = diff / (60 * 60 * 1000) % 24;
//        long days = diff / (24 * 60 * 60 * 1000);
//        String date = (days * 24 + hours) + " час " +mins+ " мин";
        String date = formDateSeries.format(inpVals.get(inpVals.size()-1).getDate().getTime());
        if(detailChk.isSelected()) {detSeries.getData().add(new XYChart.Data(date, inpVals.get(inpVals.size()-1).getTempDetail()));}
        //Температура в камере
        if(camChk.isSelected()) {camSeries.getData().add(new XYChart.Data(date, inpVals.get(inpVals.size()-1).getTempCamera()));}
        //Температура диссоциатора
        if(dissChk.isSelected()) {dissSeries.getData().add(new XYChart.Data(date, inpVals.get(inpVals.size()-1).getTempDiss()));}
        //Давление в камере
        pressSeries.getData().add(new XYChart.Data(date, inpVals.get(inpVals.size()-1).getPressCamera()*10));

        //создание графика
        tempChart.setAnimated(false);
        tempChart.setCreateSymbols(false);
        tempChart.setBackground(new Background(new BackgroundFill(
                Color.WHITE,
                new CornerRadii(0),
                new Insets(0))));
        tempChart.getData().clear();

        percChart.setAnimated(false);
        percChart.setCreateSymbols(false);
        percChart.setBackground(new Background(new BackgroundFill(
                Color.WHITE,
                new CornerRadii(0),
                new Insets(0))));
        percChart.getData().clear();

        if(detailChk.isSelected()) {tempChart.getData().add(detSeries);}
        if(camChk.isSelected()) {tempChart.getData().add(camSeries);}
        if(dissChk.isSelected()) {tempChart.getData().add(dissSeries);}

        percChart.getData().add(pressSeries);
    }
    //отправка PDF на печать
    private void printPDF(PDDocument pdfile){
        //PRINT
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPageable(new PDFPageable(pdfile));
        printJob.setJobName("printFile");
        if (printJob.printDialog()) {
            try {
                printJob.print();
                pdfile.close();
            } catch (Exception prt) {
                prt.printStackTrace();
            }
        }
    }

}
