package net.lingmar.web.italker.push.factory;

import net.lingmar.web.italker.push.bean.db.GroupMember;
import net.lingmar.web.italker.push.utils.Hib;

public class GroupMemberFactory {
    // 查询一个群成员
    public static GroupMember findById(String memberId) {
        return Hib.query(session -> session.get(GroupMember.class, memberId));
    }

    public static GroupMember update(GroupMember member) {
        return Hib.query(session -> {
            session.saveOrUpdate(member);
            return member;
        });
    }
}
