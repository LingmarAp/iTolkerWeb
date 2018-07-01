package net.lingmar.web.italker.push.service;

import com.google.common.base.Strings;
import net.lingmar.web.italker.push.bean.api.base.ResponseModel;
import net.lingmar.web.italker.push.bean.api.group.GroupCreateModel;
import net.lingmar.web.italker.push.bean.api.group.GroupJoinModel;
import net.lingmar.web.italker.push.bean.api.group.GroupMemberAddModel;
import net.lingmar.web.italker.push.bean.api.group.GroupMemberUpdateModel;
import net.lingmar.web.italker.push.bean.card.ApplyCard;
import net.lingmar.web.italker.push.bean.card.GroupCard;
import net.lingmar.web.italker.push.bean.card.GroupMemberCard;
import net.lingmar.web.italker.push.bean.card.UserCard;
import net.lingmar.web.italker.push.bean.db.Apply;
import net.lingmar.web.italker.push.bean.db.Group;
import net.lingmar.web.italker.push.bean.db.GroupMember;
import net.lingmar.web.italker.push.bean.db.User;
import net.lingmar.web.italker.push.factory.*;
import net.lingmar.web.italker.push.provider.LocalDateTimeConverter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/group")
public class GroupService extends BaseService {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<GroupCard> create(GroupCreateModel model) {
        if (!GroupCreateModel.check(model))
            return ResponseModel.buildParameterError();

        // 创建者
        User creator = getSelf();
        // 将创建者移出用户列表
        model.getUsers().remove(creator);
        if (model.getUsers().size() == 0)
            return ResponseModel.buildNoPermissionError();

        // 检查群名称是否重复
        if (GroupFactory.findByName(model.getName()) != null)
            return ResponseModel.buildHaveNameError();

        List<User> users = new ArrayList<>();
        for (String s : model.getUsers()) {
            User user = UserFactory.findById(s);
            if (user == null)
                continue;
            users.add(user);
        }
        // 没有一个成员
        if (users.size() == 0)
            return ResponseModel.buildNoPermissionError();

        Group group = GroupFactory.create(creator, model, users);
        if (group == null)
            return ResponseModel.buildServiceError();

        // 拿群管理员信息（自己的）
        GroupMember creatorMember = GroupFactory.getMember(creator.getId(), group.getId());
        if (creatorMember == null)
            return ResponseModel.buildServiceError();
        // 给所有加入群的成员发送被添加到群的通知
        Set<GroupMember> members = GroupFactory.getMembers(group);
        if (members == null)
            return ResponseModel.buildServiceError();

        members = members.stream()
                .filter(groupMember -> !groupMember.getId().equalsIgnoreCase(creatorMember.getId()))
                .collect(Collectors.toSet());
        // 开始发起推送
        PushFactory.pushJoinGroup(members);

        return ResponseModel.buildOk(new GroupCard(creatorMember));
    }

    /**
     * 查找群，没有传递参数就是搜索最近所有的群
     *
     * @param name 群名称（模糊）
     * @return 群列表
     **/
    @GET
    @Path("/search/{name:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupCard>> search(
            @DefaultValue("") @PathParam("name") String name) {
        User self = getSelf();
        List<Group> groups = GroupFactory.search(name);
        if (groups != null && groups.size() > 0) {
            List<GroupCard> groupCards = groups.stream()
                    .map(group -> {
                        GroupMember member = GroupFactory.getMember(self.getId(), group.getId());
                        return new GroupCard(group, member);
                    }).collect(Collectors.toList());
            return ResponseModel.buildOk(groupCards);
        }

        return ResponseModel.buildOk();
    }

    /**
     * 拉取自己当前的群列表
     *
     * @param dateStr 时间字段
     *                不传递->返回全部当前的群列表；
     *                传递->返回这个时间之后加入的群
     * @return 群信息列表
     */
    @GET
    @Path("/list/{date:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupCard>> list(
            @DefaultValue("") @PathParam("date") String dateStr) {
        User self = getSelf();

        LocalDateTime dateTime = null;
        if (!Strings.isNullOrEmpty(dateStr)) {
            try {
                dateTime = LocalDateTime.parse(dateStr, LocalDateTimeConverter.FORMATTER);
            } catch (Exception e) {
                dateTime = null;
            }
        }

        Set<GroupMember> members = GroupFactory.getMembers(self);
        if (members == null)
            return ResponseModel.buildOk();

        final LocalDateTime finalDateTime = dateTime;
        List<GroupCard> groupCards = members.stream()
                .filter(groupMember -> finalDateTime == null ||
                        groupMember.getUpdateAt().isAfter(finalDateTime))
                .map(GroupCard::new)
                .collect(Collectors.toList());

        return ResponseModel.buildOk(groupCards);
    }

    /**
     * 获取一个群的信息，必须是群的成员
     *
     * @param id groupId
     * @return ResponseModel<GroupCard>
     */
    @GET
    @Path("/{groupId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<GroupCard> getGroup(@PathParam("groupId") String id) {
        if (Strings.isNullOrEmpty(id))
            return ResponseModel.buildParameterError();

        User self = getSelf();
        GroupMember member = GroupFactory.getMember(self.getId(), id);
        if (member == null)
            return ResponseModel.buildNotFoundGroupError(null);

        return ResponseModel.buildOk(new GroupCard(member));
    }

    /**
     * 拉取一个群的所有成员，你必须是成员之一
     *
     * @param groupId 群id
     * @return 成员列表
     **/
    @GET
    @Path("/{groupId}/members")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupMemberCard>> members(@PathParam("groupId") String groupId) {
        User self = getSelf();

        // 检查群
        Group group = GroupFactory.findById(groupId);
        if (group == null)
            return ResponseModel.buildNotFoundGroupError(null);

        // 检查权限
        GroupMember member = GroupFactory.getMember(self.getId(), groupId);
        if (member == null)
            return ResponseModel.buildNoPermissionError();

        Set<GroupMember> members = GroupFactory.getMembers(group);
        if (members == null)
            return ResponseModel.buildServiceError();

        List<GroupMemberCard> memberCards = members.stream()
                .map(GroupMemberCard::new)
                .collect(Collectors.toList());

        return ResponseModel.buildOk(memberCards);
    }

    /**
     * 给群添加成员的接口
     *
     * @param groupId 群Id，你必须是这个群的管理者之一
     * @param model   添加群的Model
     * @return 添加成员列表
     */
    @POST
    @Path("/{groupId}/member")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<GroupMemberCard>> addMember(@PathParam("groupId") String groupId,
                                                          GroupMemberAddModel model) {
        User self = getSelf();

        if (Strings.isNullOrEmpty(groupId) || !GroupMemberAddModel.check(model))
            return ResponseModel.buildParameterError();

        model.getUsers().remove(self.getId());
        if (model.getUsers().size() == 0)
            return ResponseModel.buildParameterError();

        // 群必须存在
        Group group = GroupFactory.findById(groupId);
        if (group == null)
            return ResponseModel.buildNotFoundGroupError(null);

        // 必须为成员，同时是管理员及以上级别
        GroupMember member = GroupFactory.getMember(self.getId(), groupId);
        if (member == null || member.getPermissionType() == GroupMember.PERMISSION_TYPE_NONE)
            return ResponseModel.buildNoPermissionError();

        Set<GroupMember> oldMembers = GroupFactory.getMembers(group);
        Set<String> oldMemberUserIds = oldMembers.stream()
                .map(GroupMember::getUserId)
                .collect(Collectors.toSet());

        Set<User> insertUsers = new HashSet<>();
        for (String s : model.getUsers()) {
            User user = UserFactory.findById(s);
            if (user == null)
                continue;
            if (oldMemberUserIds.contains(s))
                continue;

            insertUsers.add(user);
        }
        // 没有一个新增的成员
        if (insertUsers.size() == 0)
            return ResponseModel.buildParameterError();

        // 进行添加操作
        Set<GroupMember> insertMembers = GroupFactory.addMembers(group, insertUsers);
        if (insertMembers == null)
            return ResponseModel.buildServiceError();

        List<GroupMemberCard> insertCards = insertMembers.stream()
                .map(GroupMemberCard::new)
                .collect(Collectors.toList());

        // 通知操作
        // 1.通知新增的成员，被加入群
        PushFactory.pushJoinGroup(insertMembers);
        // 2.通知老的成员，有XXX加入了群
        PushFactory.pushGroupMemberAdd(oldMembers, insertCards);

        return ResponseModel.buildOk(insertCards);
    }

    /**
     * 更改成员信息，请求的人必须是管理员或成员本人之一
     *
     * @param memberId 成员Id，可以查询对应的群和人
     * @param model    修改的Model
     * @return 当前成员的信息
     */
    @PUT
    @Path("/modify/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<GroupMemberCard> modifyMember(@PathParam("memberId") String memberId,
                                                       GroupMemberUpdateModel model) {
        User self = getSelf();
        if (Strings.isNullOrEmpty(memberId) || !GroupMemberUpdateModel.check(model))
            return ResponseModel.buildParameterError();

        GroupMember member = GroupMemberFactory.findById(memberId);
        if (member == null)
            return ResponseModel.buildNotFoundGroupMemberError(null);

        // 筛选出群里的管理员ID
        Set<String> admins = getAdminMembers(member.getGroup())
                .stream()
                .map(GroupMember::getUserId)
                .collect(Collectors.toSet());
        // 请求的人必须是管理员或成员本人之一
        if (!(self.getId().equalsIgnoreCase(member.getUserId()) || admins.contains(self.getId())))
            return ResponseModel.buildNoPermissionError();

        member = model.updateToMember(member);
        member = GroupMemberFactory.update(member);
        if (member == null)
            return ResponseModel.buildServiceError();

        return ResponseModel.buildOk(new GroupMemberCard(member));
    }

    /**
     * 加入一个群
     * 判断用户是已加入群，如果没有则向该群管理员发送推送
     * 管理员在客户端调用addMember的接口进行成员添加
     *
     * @param groupId 要加入的群ID
     * @return ResponseModel<ApplyCard>
     */
    @POST
    @Path("/applyJoin/{groupId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<ApplyCard> join(@PathParam("groupId") String groupId,
                                         GroupJoinModel model) {
        User self = getSelf();
        if (Strings.isNullOrEmpty(groupId) || !GroupJoinModel.check(model))
            return ResponseModel.buildParameterError();

        // 检查群
        Group group = GroupFactory.findById(groupId);
        if (group == null)
            return ResponseModel.buildNotFoundGroupError(null);

        // 申请加入群的用户已经在群里
        GroupMember member = GroupFactory.getMember(self.getId(), groupId);
        if (member != null)
            return ResponseModel.buildHaveAccountError();

        // 保存记录
        Apply apply = ApplyFactory.save(model, self, groupId, Apply.TYPE_ADD_GROUP);
        if (apply == null)
            return ResponseModel.buildServiceError();

        // 筛选出群里的管理员
        Set<GroupMember> adminMembers = getAdminMembers(group);
        // 通知管理员，有用户想要加入群
        PushFactory.pushGroupAdminJoin(adminMembers, new UserCard(self));

        // 转换
        ApplyCard applyCard = new ApplyCard(apply);

        return ResponseModel.buildOk(applyCard);
    }

    // 筛选出群里的管理员
    private Set<GroupMember> getAdminMembers(Group group) {
        Set<GroupMember> adminMembers = new HashSet<>();
        Set<GroupMember> members = GroupFactory.getMembers(group);
        for (GroupMember groupMember : members) {
            if (groupMember.getPermissionType() != GroupMember.PERMISSION_TYPE_NONE)
                adminMembers.add(groupMember);
        }

        return adminMembers;
    }
}
