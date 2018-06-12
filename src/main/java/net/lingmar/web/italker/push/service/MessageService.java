package net.lingmar.web.italker.push.service;

import com.google.common.base.Strings;
import net.lingmar.web.italker.push.bean.api.account.AccountRspModel;
import net.lingmar.web.italker.push.bean.api.account.LoginModel;
import net.lingmar.web.italker.push.bean.api.base.ResponseModel;
import net.lingmar.web.italker.push.bean.api.message.MessageCreateModel;
import net.lingmar.web.italker.push.bean.card.MessageCard;
import net.lingmar.web.italker.push.bean.db.Group;
import net.lingmar.web.italker.push.bean.db.Message;
import net.lingmar.web.italker.push.bean.db.User;
import net.lingmar.web.italker.push.factory.GroupFactory;
import net.lingmar.web.italker.push.factory.MessageFactory;
import net.lingmar.web.italker.push.factory.PushFactory;
import net.lingmar.web.italker.push.factory.UserFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * 消息发送的入口
 */
@Path("/msg")
public class MessageService extends BaseService {

    // 发送一条消息到服务器
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<MessageCard> login(MessageCreateModel model) {
        if (!MessageCreateModel.check(model)) {
            return ResponseModel.buildParameterError();
        }

        User self = getSelf();

        // 查询是否已经在数据库中
        Message message = MessageFactory.findById(model.getId());
        if(message != null)
            return ResponseModel.buildOk(new MessageCard(message));

        if(model.getReceiverType() == Message.RECEIVER_TYPE_GROUP) {
            return pushToGroup(self, model);
        } else {
            return pushToUser(self, model);
        }
    }

    // 发送到人
    private ResponseModel<MessageCard> pushToUser(User sender, MessageCreateModel model) {
        User receiver = UserFactory.findById(model.getReceiverId());
        // 没有找到接受者
        if(receiver == null)
            return ResponseModel.buildNotFoundUserError("Con't find receiver user");

        // 如果接受者和发送者是同一个人
        if(receiver.getId().equals(sender.getId()))
            return ResponseModel.buildCreateError(ResponseModel.ERROR_CREATE_MESSAGE);

        Message message = MessageFactory.add(sender, receiver, model);

        return buildAndPushResponse(sender, message);
    }

    // 发送到群
    private ResponseModel<MessageCard> pushToGroup(User sender, MessageCreateModel model) {
        Group group = GroupFactory.findById(model.getReceiverId());
        return null;
    }

    // 推送构建并返回信息
    private ResponseModel<MessageCard> buildAndPushResponse(User sender, Message message) {
        // 存储数据库失败
        if(message == null)
            return ResponseModel.buildCreateError(ResponseModel.ERROR_CREATE_MESSAGE);

        // 进行推送
        PushFactory.pushNewMessage(sender, message);

        return ResponseModel.buildOk(new MessageCard(message));
    }

}
