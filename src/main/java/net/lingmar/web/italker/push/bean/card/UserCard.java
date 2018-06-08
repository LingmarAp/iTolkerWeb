package net.lingmar.web.italker.push.bean.card;

import com.google.gson.annotations.Expose;
import net.lingmar.web.italker.push.bean.db.User;

import java.time.LocalDateTime;

public class UserCard {

    @Expose
    private String id;
    // 用户名
    @Expose
    private String name;
    // 电话
    @Expose
    private String phone;
    // 头像
    @Expose
    private String portrait;
    // 描述
    @Expose
    private String description;
    // 性别
    @Expose
    private int sex = 0;

    // 用户关注人的数量
    @Expose
    private int follows;

    // 用户粉丝数的数量
    @Expose
    private int following;

    // 我与当前User的关系状态，是否已经关注
    @Expose
    private boolean isFollow;

    // 用户信息最后更新的时间
    @Expose
    private LocalDateTime modifyAt = LocalDateTime.now();

    public UserCard(final User user) {
        this(user, false);
    }

    public UserCard(final User user, boolean isFollow) {
        this.id = user.getId();
        this.name = user.getName();
        this.phone = user.getPhone();
        this.portrait = user.getPortrait();
        this.description = user.getDescription();
        this.sex = user.getSex();
        this.modifyAt = user.getUpdateAt();
        this.isFollow = isFollow;

        // TODO 得到关注人和粉丝的数量
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public int getFollows() {
        return follows;
    }

    public void setFollows(int follows) {
        this.follows = follows;
    }

    public int getFollowing() {
        return following;
    }

    public void setFollowing(int following) {
        this.following = following;
    }

    public boolean isFollow() {
        return isFollow;
    }

    public void setFollow(boolean follow) {
        isFollow = follow;
    }

    public LocalDateTime getModifyAt() {
        return modifyAt;
    }

    public void setModifyAt(LocalDateTime modifyAt) {
        this.modifyAt = modifyAt;
    }
}
