package net.lingmar.web.italker.push.service;

import com.google.common.base.Strings;
import net.lingmar.web.italker.push.bean.api.account.AccountRspModel;
import net.lingmar.web.italker.push.bean.api.account.LoginModel;
import net.lingmar.web.italker.push.bean.api.account.RegisterModel;
import net.lingmar.web.italker.push.bean.api.base.ResponseModel;
import net.lingmar.web.italker.push.bean.db.User;
import net.lingmar.web.italker.push.factory.UserFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/account")
public class AccountService extends BaseService {

    // 登录
    @POST
    @Path("/login")
    // 指定请求与返回的相应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> login(LoginModel model) {
        if (!LoginModel.check(model)) {
            // 返回参数异常
            return ResponseModel.buildParameterError();
        }

        User user = UserFactory.login(model.getAccount(), model.getPassword());
        if (user != null) {
            // 如果携带PushId
            if (!Strings.isNullOrEmpty(model.getPushId())) {
                return bind(user, model.getPushId());
            }

            // 返回当前用户
            AccountRspModel rspModel = new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);
        } else {
            // 登录失败
            return ResponseModel.buildLoginError();
        }
    }

    // 注册
    @POST
    @Path("/register")
    // 指定请求与返回的相应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> register(RegisterModel model) {
        if (!RegisterModel.check(model)) {
            // 返回参数异常
            return ResponseModel.buildParameterError();
        }

        User user = UserFactory.findByPhone(model.getAccount().trim());
        if (user != null) {
            // 已有账户
            return ResponseModel.buildHaveAccountError();
        }

        user = UserFactory.findByName(model.getName().trim());
        if (user != null) {
            // 已有用户名
            return ResponseModel.buildHaveNameError();
        }

        user = UserFactory.register(model.getAccount(),
                model.getPassword(),
                model.getName());

        if (user != null) {
            // 如果携带PushId
            if (!Strings.isNullOrEmpty(model.getPushId())) {
                return bind(user, model.getPushId());
            }

            // 返回当前账户
            AccountRspModel rspModel = new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);
        } else {
            // 注册异常
            return ResponseModel.buildRegisterError();
        }
    }

    // 绑定设备ID
    @POST
    @Path("/bind/{pushId}")
    // 指定请求与返回的相应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> bind(@PathParam("pushId") String pushId) {
        if (Strings.isNullOrEmpty(pushId)) {
            // 返回参数异常
            return ResponseModel.buildParameterError();
        }

//        User user = UserFactory.findByToken(token);
        User self = getSelf();
        return bind(self, pushId);
    }

    /**
     * 绑定的操作
     *
     * @param self
     * @param pushId
     * @return
     */
    private ResponseModel<AccountRspModel> bind(User self, String pushId) {
        // 进行设备Id绑定操作
        User user = UserFactory.bindPushId(self, pushId);

        if (user == null) {
            // 绑定失败
            return ResponseModel.buildServiceError();
        }

        // 返回当前用户，已经绑定了
        AccountRspModel rspModel = new AccountRspModel(user, true);
        return ResponseModel.buildOk(rspModel);
    }

}
