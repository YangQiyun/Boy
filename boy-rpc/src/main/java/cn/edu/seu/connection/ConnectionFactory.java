package cn.edu.seu.connection;

import cn.edu.seu.exception.RemotingException;
import cn.edu.seu.rpc.EndPoint;

public interface ConnectionFactory {

    void init();

    Connection createConnection(EndPoint endPoint) throws RemotingException;

    Connection createConnection(String targetIP, int targetPort, int connectTimeout)
            throws Exception;


}
