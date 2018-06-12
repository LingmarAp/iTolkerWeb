package net.lingmar.web.italker.push.bean.api.message;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;
import net.lingmar.web.italker.push.bean.api.account.LoginModel;
import net.lingmar.web.italker.push.bean.db.Message;

/**
 * API请求的Model格式
 */
public class MessageCreateModel {
    // 客户端生成的UUID
    @Expose
    private String id;
    @Expose
    private String content;
    @Expose
    private String attach;

    // 消息类型
    @Expose
    private int type = Message.TYPE_STR;

    // 接受者
    @Expose
    private String receiverId;

    // 接受类型
    @Expose
    private int receiverType = Message.RECEIVER_TYPE_NONE;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAttach() {
        return attach;
    }

    public void setAttach(String attach) {
        this.attach = attach;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public int getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(int receiverType) {
        this.receiverType = receiverType;
    }

    // 参数校验
    public static boolean check(MessageCreateModel model) {
        return  model != null
                && !(Strings.isNullOrEmpty(model.id)
                || Strings.isNullOrEmpty(model.content)
                || Strings.isNullOrEmpty(model.receiverId))

                && (model.receiverType == Message.RECEIVER_TYPE_GROUP
                || model.receiverType == Message.RECEIVER_TYPE_NONE)

                && (model.type == Message.TYPE_STR
                || model.type == Message.TYPE_AUDIO
                || model.type == Message.TYPE_FILE
                || model.type == Message.TYPE_PIC);
    }
}
