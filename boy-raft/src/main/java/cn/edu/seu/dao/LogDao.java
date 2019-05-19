package cn.edu.seu.dao;

import lombok.Data;

@Data
public class LogDao {

    private long logindex;
    private byte[] data;
    private long term;
}
