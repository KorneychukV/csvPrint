package ru.vsplatforma.evt85.csvPrint;

import com.opencsv.bean.CsvDate;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Date;

public class TablePOJO {

    //@CsvBindByName(column = "Дата", required = true)
    @CsvDate("dd.MM.yyyy HH:mm:ss")
    private Date date;
    //@CsvBindByName(column = "Тип события", required = true)
    private String eventMsg;
    //@CsvBindByName(column = "Температура детали", required = true)
    private String typeEvent;
    //@CsvBindByName(column = "Температура в камере", required = true)
    private ImageView eventIcon;


    public TablePOJO(Date date, String eventMsg, String typeEvent){
        setDate(date);
        setEventMsg(eventMsg);



        switch (typeEvent){
            case "Info": setEventIcon(new ImageView(new Image("info.png"))); break;
            case "MMatch": setEventIcon(new ImageView(new Image("warning.png"))); break;
            case "BlSys": setEventIcon(new ImageView(new Image("error.png"))); break;
            default: break;
        }

    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getEventMsg() {
        return eventMsg;
    }

    public void setEventMsg(String eventMsg) {
        this.eventMsg = eventMsg;
    }

    public String getTypeEvent() {
        return typeEvent;
    }

    public void setTypeEvent(String typeEvent) {
        this.typeEvent = typeEvent;
    }

    public ImageView getEventIcon() {
        return eventIcon;
    }

    public void setEventIcon(ImageView eventIcon) {
        this.eventIcon = eventIcon;
    }
}
