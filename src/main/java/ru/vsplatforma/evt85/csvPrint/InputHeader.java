package ru.vsplatforma.evt85.csvPrint;

public class InputHeader {

    private String headerName;

    public InputHeader(String val){
        setHeaderName(val);
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String toString() {
        return "InputHeader{" +
                "headerName='" + headerName + '\'' +
                '}';
    }
}
