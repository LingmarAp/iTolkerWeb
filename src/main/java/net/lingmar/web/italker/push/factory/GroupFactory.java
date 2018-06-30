package net.lingmar.web.italker.push.factory;

import com.google.common.base.Strings;
import net.lingmar.web.italker.push.bean.api.group.GroupCreateModel;
import net.lingmar.web.italker.push.bean.db.Group;
import net.lingmar.web.italker.push.bean.db.GroupMember;
import net.lingmar.web.italker.push.bean.db.User;
import net.lingmar.web.italker.push.utils.Hib;

import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupFactory {
    // 查询一个群
    public static Group findById(String receiverId) {
        return Hib.query(session -> session.get(Group.class, receiverId));
    }

    // 查询一个群，同时user得是这个群的成员
    public static Group findById(User user, String groupId) {
        GroupMember member = getMember(user.getId(), groupId);
        if (member != null)
            return member.getGroup();

        return null;
    }

    public static Group findByName(String name) {
        return Hib.query(session -> (Group) session
                .createQuery("from Group where lower(name)=:name")
                .setParameter("name", name.toLowerCase())
                .uniqueResult());
    }

    // 获取一个群的所有成员
    public static Set<GroupMember> getMembers(Group group) {
        return Hib.query(session -> {
            @SuppressWarnings("unchecked")
            List<GroupMember> members = session
                    .createQuery("from GroupMember where group=:group")
                    .setParameter("group", group)
                    .list();

            return new HashSet<>(members);
        });
    }

    // 获取一个人加入的所有群
    public static Set<GroupMember> getMembers(User self) {
        return Hib.query(session -> {
            @SuppressWarnings("unchecked")
            List<GroupMember> members = session
                    .createQuery("from GroupMember where user=:user")
                    .setParameter("user", self)
                    .list();

            return new HashSet<>(members);
        });
    }

    // 创建群
    public static Group create(User creator, GroupCreateModel model, List<User> users) {
        return Hib.query(session -> {
            Group group = new Group(creator, model);
            session.save(group);

            GroupMember creatorMember = new GroupMember(creator, group);
            creatorMember.setPermissionType(GroupMember.PERMISSION_TYPE_ADMIN_SU);
            session.save(creatorMember);

            for (User user : users) {
                GroupMember member = new GroupMember(user, group);
                member.setPermissionType(GroupMember.PERMISSION_TYPE_NONE);
                session.save(member);
            }
            return group;
        });
    }

    // 获取一个群成员
    public static GroupMember getMember(String userId, String groupId) {
        return Hib.query(session -> (GroupMember) session
                .createQuery("from GroupMember where userId=:userId and groupId=:groupId")
                .setParameter("userId", userId)
                .setParameter("groupId", groupId)
                .uniqueResult());
    }

    // 搜索群
    public static List<Group> search(String name) {
        if (Strings.isNullOrEmpty(name)) {
            name = "";  // 保证不能为null的情况
        }
        final String searchName = "%" + name + "%";

        //noinspection unchecked
        return Hib.query(session -> (List<Group>) session
                .createQuery("from Group where lower(name) like :name ")
                .setParameter("name", searchName)
                .setMaxResults(20)
                .list());
    }

    // 给群添加成员
    public static Set<GroupMember> addMembers(Group group, Set<User> insertUsers) {
        return Hib.query(session -> {
            Set<GroupMember> members = new HashSet<>();
            for (User user : insertUsers) {
                GroupMember member = new GroupMember(user, group);
                // 保存并添加到数据库
                session.save(member);
                members.add(member);
            }

            return members;
        });
    }
}
