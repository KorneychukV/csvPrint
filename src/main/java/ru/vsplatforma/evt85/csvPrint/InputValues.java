package ru.vsplatforma.evt85.csvPrint;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;

import java.util.Date;

public class InputValues {
    //@CsvBindByName(column = "Дата", required = true)
    @CsvDate("dd.MM.yyyy HH:mm:ss")
    private Date date;
    //@CsvBindByName(column = "Тип события", required = true)
    private String typeEvent;
    //@CsvBindByName(column = "Температура детали", required = true)
    private Float tempDetail;
    //@CsvBindByName(column = "Температура в камере", required = true)
    private Float tempCamera;
    //@CsvBindByName(column = "Температура диссоциатора", required = true)
    private Float tempDiss;
    //@CsvBindByName(column = "Давление в камере", required = true)
    private Float pressCamera;

    public InputValues(Date date, String typeEvent, Float tempDetail, Float tempCamera, Float tempDiss,
        Float pressCamera){
        setDate(date);
        setTypeEvent(typeEvent);
        setTempDetail(tempDetail);
        setTempCamera(tempCamera);
        setTempDiss(tempDiss);
        setPressCamera(pressCamera);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTypeEvent() {
        return typeEvent;
    }

    public void setTypeEvent(String typeEvent) {
        this.typeEvent = typeEvent;
    }

    public Float getTempDetail() {
        return tempDetail;
    }

    public void setTempDetail(Float tempDetail) {
        this.tempDetail = tempDetail;
    }

    public Float getTempCamera() {
        return tempCamera;
    }

    public void setTempCamera(Float tempCamera) {
        this.tempCamera = tempCamera;
    }

    public Float getTempDiss() {
        return tempDiss;
    }

    public void setTempDiss(Float tempDiss) {
        this.tempDiss = tempDiss;
    }

    public Float getPressCamera() {
        return pressCamera;
    }

    public void setPressCamera(Float pressCamera) {
        this.pressCamera = pressCamera;
    }
}

