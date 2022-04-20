package cn.tybblog.touchfish.entity;

import java.util.List;

public class Chapter {
    private String url;
    private String title;
    private Integer row;
    private List<String> rowContents;

    public Chapter() {
    }

    public List<String> getRowContents() {
        return rowContents;
    }

    public void setRowContents(List<String> rowContents) {
        this.rowContents = rowContents;
        this.row = -1;
    }

    public Chapter(String url, String title) {
        this.url = url;
        this.title = title;
        this.row = -1;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public String next(){
        if(null == rowContents){
            return null;
        }
        int index = nextRow();

        if(index >= rowContents.size()){
            row = -1;
            return null;
        }
        return rowContents.get(index);
    }

    public String pre(){
        int index = preRow();
        if(index > rowContents.size()){
            row = -1;
            return null;
        }
        return rowContents.get(Math.max(0, index));
    }

    public int nextRow() {
        return ++row;
    }

    public int preRow() {
        if (row < 0) {
            return row;
        }
        return --row;
    }

    @Override
    public String toString() {
        return title.trim();
    }
}
