package cn.edu.seu;

import cn.edu.seu.conf.NodeOptions;
import cn.edu.seu.dao.LogDao;
import cn.edu.seu.dao.LogDaoWithTable;
import cn.edu.seu.rpc.EndPoint;
import cn.edu.seu.rpc.server.RpcServer;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

public class TestElectServer2 {

    public static void main(String[] args) {
        NodeOptions nodeOptions = new NodeOptions();
        EndPoint endPoint1 = new EndPoint("127.0.0.1", 8901, 2);
        EndPoint endPoint2 = new EndPoint("127.0.0.1", 8902, 2);
        EndPoint endPoint3 = new EndPoint("127.0.0.1", 8903, 2);
        EndPoint endPoint4 = new EndPoint("127.0.0.1",8904,2);
        nodeOptions.getConfiguration().addServerNode(endPoint1,1);
        nodeOptions.getConfiguration().addServerNode(endPoint2,2);
        nodeOptions.getConfiguration().addServerNode(endPoint3,3);
        nodeOptions.getConfiguration().addServerNode(endPoint4,4);
        nodeOptions.setServerId(2);

        RpcServer rpcServer = new RpcServer(endPoint2);

        RaftGroupService raftGroupService = new RaftGroupService("raft", nodeOptions, 2, rpcServer);
        raftGroupService.start();

        synchronized (TestElect.class) {
            try {
                TestElect.class.wait();
            } catch (Throwable e) {

            }
        }
    }


    public static void main2(String[] args) {
        //读取配置文件
        InputStream is = TestElectServer2.class.getClassLoader().getResourceAsStream("config.xml");
        //初始化mybatis，创建SqlSessionFactory类实例
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        //创建Session实例
        SqlSession session = sqlSessionFactory.openSession();

        for (int i = 21; i < 41; i++) {



            Map<String, Object> map = new HashMap<>();
            map.put("tableName", "server1");
            map.put("logindex", 40L);
            //查询数据
//        LogDao[] t = new LogDao[]{null};
//        try {
//
//
//            session.select("getLogByTable", map, (resultContext) -> {
//                t[0] = (LogDao) resultContext.getResultObject();
//                System.out.println(t[0].getTerm());
//            });
//        }catch (Exception e){
//            System.out.println(e.getMessage());
//        }
//        if(t[0]==null){
//            System.out.println("null");
//        }

            LogDaoWithTable logDaoTable = new LogDaoWithTable();
            logDaoTable.setTableName("server8901");
            logDaoTable.setData(new String("woc1da./ao").getBytes());
            logDaoTable.setLogindex(i);
            logDaoTable.setTerm(1);
            int result = 0;
            try {
                result = session.insert("insertByTable", logDaoTable);

            } catch (PersistenceException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                System.out.println(e.getClass().getSimpleName());
                System.out.println(e.getMessage());
            }
            if (result != 1) {
                System.out.println("error");
            }
            System.out.println("now result is " + result);

        }
        //提交事务
        session.commit();
        //关闭Session
        session.close();
    }
}
