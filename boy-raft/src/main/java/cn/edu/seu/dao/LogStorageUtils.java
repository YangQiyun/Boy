package cn.edu.seu.dao;

import exception.EmptyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LogStorageUtils {

    public static LogStorageUtils INSTANCE = new LogStorageUtils();

    private static volatile SqlSessionFactory sqlSessionFactory;

    public LogStorageUtils() {
        //读取配置文件
        InputStream is = LogStorageUtils.class.getClassLoader().getResourceAsStream("config.xml");
        //初始化mybatis，创建SqlSessionFactory类实例
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
    }

    public static boolean insertLog(String tableName, long logIndex, long term, byte[] data) {
        LogDaoWithTable logDaoWithTable = new LogDaoWithTable(tableName, logIndex, data, term);
        //创建Session实例
        SqlSession session = sqlSessionFactory.openSession();

        int result = 0;
        try {
            result = session.insert("insertByTable", logDaoWithTable);
        }catch (Exception e){
            log.error("sql error insert by log {} result is {} error message is {}", logDaoWithTable, result,e.getMessage());
            return false;
        }
        if (result != 1) {
            log.error("sql error insert by log {} result is {}", logDaoWithTable, result);
            return false;
        }
        //提交事务
        session.commit();
        //关闭Session
        session.close();
        return true;
    }


    public static LogDao findByLogIndex(String tableName, long logIndex) throws EmptyException{
        final LogDao[] result = {null};
        //创建Session实例
        SqlSession session = sqlSessionFactory.openSession();

        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableName);
        map.put("logindex", logIndex);
        //查询数据
        session.select("getLogByTable", map, (resultContext) -> {
            result[0] = (LogDao) resultContext.getResultObject();
        });

        //提交事务
        session.commit();
        //关闭Session
        session.close();

        if(result[0]==null){
            throw new EmptyException("no found");
        }
        return result[0];
    }

}
