package net.lingmar.web.italker.push.service;

import com.google.common.base.Strings;
import net.lingmar.web.italker.push.bean.api.base.PushModel;
import net.lingmar.web.italker.push.bean.api.base.ResponseModel;
import net.lingmar.web.italker.push.bean.api.user.UpdateInfoModel;
import net.lingmar.web.italker.push.bean.card.UserCard;
import net.lingmar.web.italker.push.bean.db.User;
import net.lingmar.web.italker.push.factory.UserFactory;
import net.lingmar.web.italker.push.utils.PushDispatcher;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户信息处理Service
 */
@Path("/user")
public class UserService extends BaseService {

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> update(UpdateInfoModel model) {
        if (!UpdateInfoModel.check(model)) {
            return ResponseModel.buildParameterError();
        }

        User self = getSelf();
        // 更新用户信息
        self = model.updateToUser(self);
        self = UserFactory.update(self);

        if (self == null) {
            // 用户名重复
            // 更新失败
            return ResponseModel.buildHaveNameError();
        }

        return ResponseModel.buildOk(new UserCard(self, true));
    }

    // 拉取联系人
    @GET
    @Path("/contact")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<Set<UserCard>> contact() {
        User self = getSelf();

        // 拿到我的联系人
        Set<User> users = UserFactory.contact(self);
        // 转换为UserCard
        Set<UserCard> userCards = users.stream()
                .map(user -> new UserCard(user, true))
                .collect(Collectors.toSet());

        return ResponseModel.buildOk(userCards);
    }

    // 关注人
    @PUT
    @Path("/follow/{followId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> follow(@PathParam("followId") String followId) {
        User self = getSelf();

        // 不能关注自己
        if (self.getId().equalsIgnoreCase(followId)) {
            return ResponseModel.buildParameterError();
        }
        // 找到我要关注的人
        User followUser = UserFactory.findById(followId);
        if (followUser == null) {
            return ResponseModel.buildNotFoundUserError(null);
        }

        // 备注默认没有，后面进行扩展
        followUser = UserFactory.follow(self, followUser, null);
        if (followUser == null) {
            // 关注失败，返回服务器异常
            return ResponseModel.buildServiceError();
        }

        // TODO 通知我关注的人我关注了他

        // 返回关注人的信息
        return ResponseModel.buildOk(new UserCard(followUser, true));
    }

    // 获取某人的信息
    @GET
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> getUser(@PathParam("id") String id) {
        if (Strings.isNullOrEmpty(id)) {
            return ResponseModel.buildParameterError();
        }

        User self = getSelf();
        if (self.getId().equalsIgnoreCase(id)) {
            // 返回自己，不必查询数据库
            return ResponseModel.buildOk(new UserCard(self, true));
        }

        User user = UserFactory.findById(id);
        if (user == null) {
            return ResponseModel.buildNotFoundUserError(null);
        }

        // 如果我们之间有关注的记录，则我已关注查询信息的用户
        boolean isFollow = UserFactory.getUserFollow(self, user) != null;
        return ResponseModel.buildOk(new UserCard(user, isFollow));
    }

    // 搜索人的接口实现
    // 为了简化分页，只返回20条数据
    @GET
    @Path("/search/{name:(.*)?}") // 名字为任意字符，可以为空
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<Set<UserCard>> contact(@PathParam("name") String name) {
        User self = getSelf();

        // 先取出数据
        Set<User> searchUsers = UserFactory.search(name);

        // 取出我的联系人
        Set<User> contacts = UserFactory.contact(self);

        Set<UserCard> userCards = searchUsers.stream()
                .map(user -> {
                    // 判断这个人是否是自己，或者是我的联系人中的人
                    boolean isFollow = user.getId().equalsIgnoreCase(self.getId())
                            || contacts.stream().anyMatch(contactsUser -> contactsUser.getId()
                                    .equalsIgnoreCase(user.getId())
                    );

                    return new UserCard(user, isFollow);
                })
                .collect(Collectors.toSet());
        return ResponseModel.buildOk(userCards);
    }

}
