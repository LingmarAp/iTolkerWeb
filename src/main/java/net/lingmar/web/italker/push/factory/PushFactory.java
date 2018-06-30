package net.lingmar.web.italker.push.factory;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import net.lingmar.web.italker.push.bean.api.base.PushModel;
import net.lingmar.web.italker.push.bean.card.GroupMemberCard;
import net.lingmar.web.italker.push.bean.card.MessageCard;
import net.lingmar.web.italker.push.bean.card.UserCard;
import net.lingmar.web.italker.push.bean.db.*;
import net.lingmar.web.italker.push.utils.Hib;
import net.lingmar.web.italker.push.utils.PushDispatcher;
import net.lingmar.web.italker.push.utils.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            Group group = message.getGroup();
            if (group == null)
                group = GroupFactory.findById(message.getGroupId());

            if (group == null)
                return;

            // 发给群成员
            Set<GroupMember> members = GroupFactory.getMembers(group);
            if (members == null | members.size() == 0)
                return;

            members = members.stream()
                    .filter((Predicate<GroupMember>) input -> !input.getId().equalsIgnoreCase(sender.getId()))
                    .collect(Collectors.toSet());

            List<PushHistory> histories = new ArrayList<>();
            addGroupMembersPushModel(pusher, histories, members, entity, PushModel.ENTITY_TYPE_MESSAGE);

            // 保存到数据库
            Hib.queryOnly(session -> {
                for (PushHistory history : histories) {
                    session.saveOrUpdate(history);
                }
            });
        }

        pusher.submit();
    }

    /**
     * 给群成员构建一个信息
     * 把消息存储到数据库历史中，每个人每条消息都是一个记录
     *
     * @param pusher            推送的发送者
     * @param histories         数据库要存储的列表
     * @param members           要发送的用户
     * @param entity            消息字符串
     * @param entityTypeMessage 消息类型
     */
    private static void addGroupMembersPushModel(PushDispatcher pusher,
                                                 List<PushHistory> histories,
                                                 Set<GroupMember> members,
                                                 String entity,
                                                 int entityTypeMessage) {
        for (GroupMember member : members) {
            // 急加载，不需要再通过Id去数据库查找
            User receiver = member.getUser();
            if (receiver == null)
                return;

            // 历史记录表字段建立
            PushHistory history = new PushHistory();
            history.setEntityType(entityTypeMessage); // 普通消息类型
            history.setEntity(entity);
            history.setReceiver(receiver);
            history.setReceiverPushId(receiver.getPushId()); // 接受者当前的设备推送Id
            histories.add(history);

            PushModel pushModel = new PushModel();
            pushModel.add(history.getEntityType(), history.getEntity());

            pusher.add(receiver, pushModel);
        }
    }

    /**
     * 通知一些成员，被加入了XXX群
     *
     * @param members 被加入群的成员
     */
    public static void pushJoinGroup(Set<GroupMember> members) {
        // 发送者
        PushDispatcher pusher = new PushDispatcher();

        List<PushHistory> histories = new ArrayList<>();
        for (GroupMember member : members) {
            User receiver = member.getUser();
            if (receiver == null)
                return;

            // 每个成员的信息卡片
            GroupMemberCard memberCard = new GroupMemberCard(member);
            String entity = TextUtil.toJson(memberCard);

            // 历史记录表字段建立
            PushHistory history = new PushHistory();
            history.setEntityType(PushModel.ENTITY_TYPE_ADD_GROUP); // 普通消息类型
            history.setEntity(entity);
            history.setReceiver(receiver);
            history.setReceiverPushId(receiver.getPushId()); // 接受者当前的设备推送Id
            histories.add(history);

            PushModel pushModel = new PushModel();
            pushModel.add(history.getEntityType(), history.getEntity());

            pusher.add(receiver, pushModel);
        }

        // 保存到数据库
        Hib.queryOnly(session -> {
            for (PushHistory history : histories) {
                session.saveOrUpdate(history);
            }
        });

        pusher.submit();
    }

    /**
     * 通知老成员，有一系列新的成员加入到某个群
     *
     * @param oldMembers  老的成员
     * @param insertCards 新的成员信息集合
     */
    public static void pushGroupMemberAdd(Set<GroupMember> oldMembers, List<GroupMemberCard> insertCards) {
        // 发送者
        PushDispatcher pusher = new PushDispatcher();

        // 一个历史记录列表
        List<PushHistory> histories = new ArrayList<>();

        // 当前新增用户的集合的 JSON字符串
        String entity = TextUtil.toJson(insertCards);

        // 给每一个老用户构建一条信息
        addGroupMembersPushModel(pusher, histories, oldMembers,
                entity, PushModel.ENTITY_TYPE_ADD_GROUP_MEMBERS);

        // 保存到数据库
        Hib.queryOnly(session -> {
            for (PushHistory history : histories) {
                session.save(history);
            }
        });

        pusher.submit();
    }

    /**
     * 推送账户退出消息
     *
     * @param user   接受者
     * @param pushId 这个时刻接受者的设备Id
     */
    public static void pushLogout(User user, String pushId) {
        // 历史记录表字段建立
        PushHistory history = new PushHistory();
        history.setEntityType(PushModel.ENTITY_TYPE_LOGOUT); // 普通消息类型
        history.setEntity("账户在其他设备登入");
        history.setReceiver(user);
        history.setReceiverPushId(pushId); // 接受者当前的设备推送Id

        // 保存到数据库
        Hib.queryOnly(session -> session.saveOrUpdate(history));

        PushDispatcher pusher = new PushDispatcher();
        PushModel pushModel = new PushModel();
        pushModel.add(history.getEntityType(), history.getEntity());

        pusher.add(user, pushModel);
        pusher.submit();
    }

    /**
     * 给一个朋友推送我的信息
     *
     * @param receiver 接受者
     * @param userCard   我的卡片信息
     */
    public static void pushFollow(User receiver, UserCard userCard) {
        // 一定是相互关注了的
        userCard.setFollow(true);

        String entity = TextUtil.toJson(userCard);

        // 历史记录表字段建立
        PushHistory history = new PushHistory();
        history.setEntityType(PushModel.ENTITY_TYPE_ADD_FRIEND); // 普通消息类型
        history.setEntity(entity);
        history.setReceiver(receiver);
        history.setReceiverPushId(receiver.getPushId()); // 接受者当前的设备推送Id

        // 保存到数据库
        Hib.queryOnly(session -> session.save(history));

        PushDispatcher pusher = new PushDispatcher();
        PushModel pushModel = new PushModel()
                .add(history.getEntityType(), history.getEntity());
        pusher.add(receiver, pushModel);
        pusher.submit();
    }
}
