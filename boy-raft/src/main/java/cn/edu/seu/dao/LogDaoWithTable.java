package cn.edu.seu.dao;

import lombok.Data;

@Data
public class LogDaoWithTable {
    private String tableName;
    private long logindex;
    private byte[] data;
    private long term;

    public LogDaoWithTable(String tableName, long logindex, byte[] data, long term) {
        this.tableName = tableName;
        this.logindex = logindex;
        this.data = data;
        this.term = term;
    }

    public LogDaoWithTable() {
    }
}
