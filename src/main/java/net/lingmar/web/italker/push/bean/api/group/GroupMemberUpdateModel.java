package net.lingmar.web.italker.push.bean.api.group;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;
import net.lingmar.web.italker.push.bean.db.GroupMember;

public class GroupMemberUpdateModel {
    // 别名
    @Expose
    private String alias;

    // 消息的通知级别
    @Expose
    private int notifyLevel;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getNotifyLevel() {
        return notifyLevel;
    }

    public void setNotifyLevel(int notifyLevel) {
        this.notifyLevel = notifyLevel;
    }

    // 只要其中一个不为空即可
    public static boolean check(GroupMemberUpdateModel model) {
        return !(Strings.isNullOrEmpty(model.alias)
                && model.notifyLevel == 0);
    }

    public GroupMember updateToMember(GroupMember member) {
        if (!Strings.isNullOrEmpty(alias))
            member.setAlias(alias);

        if (notifyLevel != 0)
            member.setNotifyLevel(notifyLevel);

        return member;
    }
}
