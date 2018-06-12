package net.lingmar.web.italker.push.factory;

import com.google.common.base.Strings;
import net.lingmar.web.italker.push.bean.api.base.PushModel;
import net.lingmar.web.italker.push.bean.card.MessageCard;
import net.lingmar.web.italker.push.bean.db.Message;
import net.lingmar.web.italker.push.bean.db.PushHistory;
import net.lingmar.web.italker.push.bean.db.User;
import net.lingmar.web.italker.push.utils.Hib;
import net.lingmar.web.italker.push.utils.PushDispatcher;
import net.lingmar.web.italker.push.utils.TextUtil;

/**
 * 消息存储与处理工具类
 */
public class PushFactory {
    // 发送一条消息，并在当前的历史记录中存储记录
    public static void pushNewMessage(User sender, Message message) {
        if (sender == null || message == null)
            return;

        // 消息卡片用于发送
        MessageCard card = new MessageCard(message);
        // 要推送的JSON字符串
        String entity = TextUtil.toJson(card);

        // 发送者
        PushDispatcher pusher = new PushDispatcher();

        if (message.getGroup() == null
                && Strings.isNullOrEmpty(message.getGroupId())) {
            // 发给人
            User receiver = UserFactory.findById(message.getReceiverId());
            if (receiver == null)
                return;

            // 历史记录表字段建立
            PushHistory history = new PushHistory();
            history.setEntityType(PushModel.ENTITY_TYPE_MESSAGE); // 普通消息类型
            history.setEntity(entity);
            history.setSender(sender);
            history.setReceiver(receiver);
            history.setReceiverPushId(receiver.getPushId()); // 接受者当前的设备推送Id

            PushModel pushModel = new PushModel();
            pushModel.add(history.getEntityType(), history.getEntity());

            pusher.add(receiver, pushModel);

            // 数据库存储
            Hib.queryOnly(session -> session.save(history));
        } else {
            // 发给群
        }

        pusher.submit();
    }
}
