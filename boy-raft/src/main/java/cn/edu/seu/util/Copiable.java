package cn.edu.seu.util;

/**
 * 提供copy接口，并且函数是进行深拷贝
 * @param <T>
 */
public interface Copiable<T> {

    T copy();
}
