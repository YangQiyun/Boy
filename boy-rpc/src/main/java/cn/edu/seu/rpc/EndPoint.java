package cn.edu.seu.rpc;

import cn.edu.seu.util.IDGeneratorUtil;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Data
public class EndPoint {

    private String ip;

    private int port;

    // 该节点生成的链接数量
    private int connNum;

    // 唯一标识符
    private String uniqueKey;

    public EndPoint(String ip, int port, int connNum) {
        this.ip = ip;
        this.port = port;
        this.connNum = connNum;
        uniqueKey = ip + "-" + port + "-" + IDGeneratorUtil.INSTANCE.getID();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(ip)
                .append(port)
                .toHashCode();
    }

    @Override
    public boolean equals(Object object) {
        boolean flag = false;
        if (object != null && EndPoint.class.isAssignableFrom(object.getClass())) {
            EndPoint rhs = (EndPoint) object;
            flag = new EqualsBuilder()
                    .append(ip, rhs.ip)
                    .append(port, rhs.port)
                    .isEquals();
        }
        return flag;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("ENDPOINT [ ")
                .append(ip)
                .append(":")
                .append(port)
                .append(" ] ").toString();
    }
}
