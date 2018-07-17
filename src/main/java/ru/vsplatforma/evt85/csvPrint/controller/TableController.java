package ru.vsplatforma.evt85.csvPrint.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.opencsv.CSVReader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import ru.vsplatforma.evt85.csvPrint.Main;
import ru.vsplatforma.evt85.csvPrint.TablePOJO;

import javax.swing.*;
import javax.swing.text.html.ImageView;
import java.awt.print.PrinterJob;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class TableController {

    @FXML
    VBox parent;

    @FXML
    private VBox top;
    @FXML
    private VBox bottom;

    private String csvPath = "";
    private String csvName = "";


    @FXML
    private TableColumn<TablePOJO, Date> dateCol = new TableColumn<TablePOJO, Date>("Дата");
    @FXML
    private TableColumn<TablePOJO, String> msgCol = new TableColumn<TablePOJO, String>("Сообщнение");
    @FXML
    private TableColumn<TablePOJO, ImageView> eventCol = new TableColumn<TablePOJO, ImageView>("Тип события");
    private Button printTable = new Button("Напечатать таблицу");




    @FXML
    public void initialize() {

        dateCol.setMinWidth(150);
        msgCol.setMinWidth(720);
        eventCol.setMinWidth(20);
        eventCol.setStyle( "-fx-alignment: CENTER;");

        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setCellFactory((TableColumn<TablePOJO, Date> column) -> {
            return new TableCell<TablePOJO, Date>() {
                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        SimpleDateFormat formDate = new SimpleDateFormat(
                                "dd.MM.yyyy HH:mm:ss");
                        setText(formDate.format(item));
                    }
                }
            };
        });

        msgCol.setCellValueFactory(new PropertyValueFactory<>("eventMsg"));
        eventCol.setCellValueFactory(new PropertyValueFactory<>("eventIcon"));

        bottom.setAlignment(Pos.CENTER);

        printTable.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //СОЗДАТЬ РЯДОМ С csv
                createPDF("eventTable.pdf");
            }
        });
    }

    //Чтение из файла
    public void readCSV(String pathToCsv, String csvName){
        TableView<TablePOJO> evnTable = new TableView<TablePOJO>();
        ObservableList<TablePOJO> in = FXCollections.observableArrayList();
        evnTable.setPrefHeight(2000);
        bottom.getChildren().add(printTable);
        top.getChildren().add(evnTable);

        evnTable.setEditable(true);

        FileInputStream fis = null;

        try {
            this.csvName = csvName;
            this.csvPath = pathToCsv;
            fis = new FileInputStream(pathToCsv);
            InputStreamReader isr = new InputStreamReader(fis, "Windows-1251");
            CSVReader reader = new CSVReader(isr);
            String[] record;
            reader.readNext();
            while ((record = reader.readNext()) != null) {
                if (!record[3].equals("")) {
                    //Date
                    SimpleDateFormat formDate = new SimpleDateFormat(
                            "dd.MM.yyyy HH:mm:ss");
                    in.add(new TablePOJO(formDate.parse(record[0] + " " + record[1]),
                            record[3], record[2]));
                }
            }

            evnTable.setItems(in);
            evnTable.getColumns().addAll(eventCol,dateCol,msgCol);

            evnTable.setEditable(false);

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

    public void createPDF(String path){
        Document doc = new Document();
        try {
            BaseFont bfCyr = BaseFont.createFont("ARIALUNI.TTF", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font font = new Font(bfCyr, 12);
            PdfWriter.getInstance(doc, new FileOutputStream(path));
            doc.open();
            //Название таблицы

            String temp = csvName.substring(6,8) + "." + csvName.substring(4,6) + "." + csvName.substring(0,4) + " ";
            temp += csvName.substring(9,11) + ":" + csvName.substring(11,13) + ":" + csvName.substring(13,15);

            Paragraph p = new Paragraph("Таблица событий тех. процесса от " + temp, font);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingAfter(10);
            doc.add(new Paragraph(p));
            font = new Font(bfCyr, 10);
            PdfPTable table = new PdfPTable(3);
            table.setTotalWidth(new float[]{120f,180f,350f});
            //Шапка таблицы

            PdfPCell cell = new PdfPCell();
            cell.addElement(new Paragraph("Тип события", font));
            cell.setPaddingLeft(10);
            cell.setPaddingBottom(10);
            table.addCell(cell);

            cell = new PdfPCell();
            cell.addElement(new Paragraph("Дата", font));
            cell.setPaddingLeft(40);
            cell.setPaddingBottom(10);
            table.addCell(cell);

            cell = new PdfPCell();
            cell.addElement(new Paragraph("Сообщение", font));
            cell.setPaddingLeft(85);
            cell.setPaddingBottom(10);
            table.addCell(cell);
            table.completeRow();
            //Content
            InputStream infoPng = Main.class.getClassLoader().getResourceAsStream("info.png");
            InputStream warPng = Main.class.getClassLoader().getResourceAsStream("warning.png");
            InputStream errPng = Main.class.getClassLoader().getResourceAsStream("error.png");

            byte[] info = IOUtils.toByteArray(infoPng);
            byte[] war = IOUtils.toByteArray(warPng);
            byte[] err = IOUtils.toByteArray(errPng);

            FileInputStream fis = new FileInputStream(csvPath);
            InputStreamReader isr = new InputStreamReader(fis, "Windows-1251");
            CSVReader reader = new CSVReader(isr);
            String[] record;
            reader.readNext();
            while ((record = reader.readNext()) != null) {
                if (!record[3].equals("")) {
                    Image img = null;
                    switch (record[2]) {
                        case "Info":
                            img = Image.getInstance(info);
                            break;
                        case "MMatch":
                            img = Image.getInstance(war);
                            break;
                        case "BlSys":
                            img = Image.getInstance(err);
                            break;
                        default:
                            break;
                    }

                    if (img != null) {
                        img.scalePercent(60);
                    }

                    cell = new PdfPCell();
                    cell.addElement(img);
                    cell.setPaddingTop(10);
                    cell.setPaddingBottom(10);
                    cell.setPaddingLeft(30);
                    table.addCell(cell);

                    cell = new PdfPCell();
                    cell.addElement(new Paragraph(String.valueOf(record[1] + " " + record[0]), font));
                    cell.setPaddingBottom(10);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    table.addCell(cell);

                    cell = new PdfPCell();
                    if (record[2].isEmpty()) {
                        cell.addElement(new Paragraph(""));
                    } else {
                        cell.addElement(new Paragraph(record[3], font));
                    }
                    cell.setPaddingBottom(10);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    table.addCell(cell);
                    table.completeRow();
                }
            }
            doc.add(table);
            doc.close();
            printPDF();
            new File(path).delete();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printPDF(){
        File f = new File("eventTable.pdf");
        try {
            PDDocument doc = PDDocument.load(f);
            PrinterJob pj = PrinterJob.getPrinterJob();
            pj.setPageable(new PDFPageable(doc));
            pj.setJobName("printFile");
            if (pj.printDialog()) {
                try {
                    pj.print();
                    doc.close();
                } catch (Exception prt) {
                    prt.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
