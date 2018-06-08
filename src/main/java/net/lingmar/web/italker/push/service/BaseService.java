package net.lingmar.web.italker.push.service;

import net.lingmar.web.italker.push.bean.db.User;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

public class BaseService {

    @Context
    protected SecurityContext securityContext;    // 拦截器返回的上下文

    // 用户修改信息接口
    // 返回用户的个人信息
    protected User getSelf() {
        return (User) securityContext.getUserPrincipal();
    }
}
