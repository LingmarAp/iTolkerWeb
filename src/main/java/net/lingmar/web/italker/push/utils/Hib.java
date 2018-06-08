package net.lingmar.web.italker.push.utils;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Created by qiujuer
 * on 2017/2/17.
 */
public class Hib {
    // 全局SessionFactory
    private static SessionFactory sessionFactory;

    static {
        // 静态初始化sessionFactory
        init();
    }

    private static void init() {
        // 从hibernate.cfg.xml文件初始化
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure() // configures settings from hibernate.cfg.xml
                .build();
        try {
            // build 一个sessionFactory
            sessionFactory = new MetadataSources(registry)
                    .buildMetadata()
                    .buildSessionFactory();
        } catch (Exception e) {
            e.printStackTrace();
            // 错误则打印输出，并销毁
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    /**
     * 获取全局的SessionFactory
     *
     * @return SessionFactory
     */
    public static SessionFactory sessionFactory() {
        return sessionFactory;
    }

    /**
     * 从SessionFactory中得到一个Session会话
     *
     * @return Session
     */
    public static Session session() {
        return sessionFactory.getCurrentSession();
    }

    /**
     * 关闭sessionFactory
     */
    public static void closeFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    // 用户的实际操作的接口
    // 具有返回值T
    public interface Query<T> {
        T query(Session session);
    }

    public interface QueryOnly {
        void query(Session session);
    }

    public static void queryOnly(QueryOnly query) {
        // 防止Session重复，重开一个Session
        Session session = sessionFactory.openSession();

        final Transaction transaction = session.beginTransaction();
        try {
            // 调用传递进来的接口
            // 并调用接口的方法把Session传递进去
            query.query(session);
            // 提交
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            // 回滚
            transaction.rollback();
        } finally {
            session.close();
        }
    }

    public static<T> T query(Query<T> query) {
        // 防止Session重复，重开一个Session
        Session session = sessionFactory.openSession();

        final Transaction transaction = session.beginTransaction();

        T t = null;
        try {
            // 调用传递进来的接口
            // 并调用接口的方法把Session传递进去
            t = query.query(session);
            // 提交
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            // 重置t
            t = null;
            // 回滚
            transaction.rollback();
        } finally {
            session.close();
        }

        return t;
    }

}
