package net.lingmar.web.italker.push.factory;

import com.google.common.base.Strings;
import net.lingmar.web.italker.push.bean.db.User;
import net.lingmar.web.italker.push.bean.db.UserFollow;
import net.lingmar.web.italker.push.utils.Hib;
import net.lingmar.web.italker.push.utils.TextUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserFactory {

    // 通过Token字段查询用户信息
    public static User findByToken(String token) {
        return Hib.query(session -> (User) session
                .createQuery("from User where token=:token")
                .setParameter("token", token)
                .uniqueResult());
    }

    // 通过Phone查找User
    public static User findByPhone(String phone) {
        return Hib.query(session -> (User) session
                .createQuery("from User where phone=:inPhone")
                .setParameter("inPhone", phone)
                .uniqueResult());
    }

    // 通过Name查找User
    public static User findByName(String name) {
        return Hib.query(session -> (User) session
                .createQuery("from User where name=:inName")
                .setParameter("inName", name)
                .uniqueResult());
    }

    // 通过Id查找User
    public static User findById(String id) {
        return Hib.query(session -> session.get(User.class, id));
    }

    /**
     * 更新用户信息到数据库
     *
     * @param user user
     * @return User
     */
    public static User update(User user) {
        return Hib.query(session -> {
            session.saveOrUpdate(user);
            return user;
        });
    }

    /**
     * 给的当前设备绑定PushId
     *
     * @param user   user
     * @param pushId pushId
     * @return User
     */
    public static User bindPushId(User user, String pushId) {
        if (Strings.isNullOrEmpty(pushId)) {
            return null;
        }

        // 查询是否有其他账户绑定了这个设备
        // 取消绑定，避免推送混乱
        // 查询列表不包括自己
        Hib.queryOnly(session -> {
            @SuppressWarnings("unchecked")
            List<User> userList = (List<User>) session
                    .createQuery("from User where lower(pushId)=:pushId and id!=:userId")
                    .setParameter("pushId", pushId.toLowerCase())
                    .setParameter("userId", user.getId())
                    .list();

            for (User u : userList) {
                // 更新为null
                u.setPushId(null);
                session.saveOrUpdate(u);
            }
        });

        if (pushId.equalsIgnoreCase(user.getPushId())) {
            // 如果当前需要绑定的设备Id已经绑定过过了
            // 不需要额外绑定
            return user;
        } else {
            // 如果账号之前的设备Id和需要绑定的不同
            // 需要进行单点登录，让之前的设备退出账户
            // 给之前的设备推送一条退出消息
            if (Strings.isNullOrEmpty(user.getPushId())) {
                // TODO 推送一个退出消息
            }

            user.setPushId(pushId);
            return update(user);
        }
    }

    public static User login(String account, String password) {
        String accountStr = account.trim();
        String passwordStr = encodePassword(password);

        User user = Hib.query(session -> (User) session
                .createQuery("from User where phone=:phone and password=:password")
                .setParameter("phone", accountStr)
                .setParameter("password", passwordStr)
                .uniqueResult());

        if (user != null) {
            // 如果登录成功
            // 更新Token
            user = login(user);
        }

        return user;
    }

    /**
     * 用户注册
     *
     * @param account  账户（手机号）
     * @param password 密码
     * @param name     用户名
     * @return User
     */
    public static User register(String account, String password, String name) {
        // 去除账户中的首位空格
        account = account.trim();
        // 处理密码
        password = encodePassword(password);

        User user = createUser(account, password, name);
        if (user != null) {
            user = login(user);
        }

        return user;
    }

    /**
     * 注册部分的新建用户逻辑
     *
     * @param account  account
     * @param password password
     * @param name     name
     * @return User
     */
    private static User createUser(String account, String password, String name) {
        User user = new User();

        user.setName(name);
        user.setPassword(password);
        user.setPhone(account);

        return Hib.query(session -> {
            session.save(user);
            return user;
        });
    }

    /**
     * 把一个User进行登录的操作
     *
     * @param user user
     * @return User
     */
    private static User login(User user) {
        // 使用一个随机的UUID值充当Token
        String newToken = UUID.randomUUID().toString();
        // 进行一次Base64格式化
        newToken = TextUtil.encodeBase64(newToken);
        user.setToken(newToken);

        return update(user);
    }

    private static String encodePassword(String passWord) {
        passWord = passWord.trim();

        passWord = TextUtil.getMD5(passWord);
        return TextUtil.encodeBase64(passWord);
    }

    /**
     * 获取我的联系人的列表
     *
     * @param self User
     * @return List<User>
     */
    public static Set<User> contact(User self) {
        return Hib.query(session -> {
            // 重新加载一次用户信息到self中，和当前的session绑定
            session.load(self, self.getId());
            // 获取我关注的人
            Set<UserFollow> follows = self.getFollowing();

            return follows.stream()
                    .map(UserFollow::getTarget)
                    .collect(Collectors.toSet());
        });
    }

    /**
     * 关注人的操作
     * 被关注人同意操作简化为双方同时关注的操作
     *
     * @param origin 发起者
     * @param target 被关注的人
     * @param alias  备注名
     * @return 被关注的人的信息
     */
    public static User follow(final User origin, final User target, final String alias) {
        UserFollow follow = getUserFollow(origin, target);

        if (follow != null) {
            // 已关注直接返回
            return follow.getTarget();
        }

        return Hib.query(session -> {
            // 想要操作懒加载数据，需要重新load一次
            session.load(origin, origin.getId());
            session.load(target, target.getId());

            // 我关注他的时候，他也同时关注了我
            // 同时进行两条UserFollow的操作
            UserFollow originFollow = new UserFollow();
            originFollow.setOrigin(origin);
            originFollow.setTarget(target);
            // 备注是我对他的备注，他对我并没有备注
            originFollow.setAlias(alias);

            UserFollow targetFollow = new UserFollow();
            targetFollow.setOrigin(target);
            targetFollow.setTarget(origin);

            session.save(originFollow);
            session.save(targetFollow);

            return target;
        });
    }

    /**
     * 查询两个人是否已经关注
     *
     * @param origin 发起者
     * @param target 被关注人
     * @return 返回中间类UserFollow
     */
    public static UserFollow getUserFollow(final User origin, final User target) {
        return Hib.query(session -> (UserFollow) session.createQuery("from UserFollow where originId=:originId and targetId=:targetId")
                .setParameter("originId", origin.getId())
                .setParameter("targetId", target.getId())
                .setMaxResults(1)
                .uniqueResult());
    }

    /**
     * 搜索联系人的实现
     *
     * @param name 查询的name，允许为空
     * @return 查询到的用户集合，如果name为空，则返回最近的用户
     */
    @SuppressWarnings("unchecked")
    public static Set<User> search(String name) {
        if (Strings.isNullOrEmpty(name)) {
            name = "";  // 保证不能为null的情况
        }
        final String searchName = "%" + name + "%";

        return new HashSet<>(Hib.query(session -> (List<User>) session.createQuery("from User where lower(name) like :name " +
                "and portrait is not null " +
                "and description is not null ")
                .setParameter("name", searchName)
                .setMaxResults(20)
                .list()));
    }
}
