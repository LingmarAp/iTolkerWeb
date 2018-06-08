package net.lingmar.web.italker.push.provider;

import com.google.common.base.Strings;
import net.lingmar.web.italker.push.bean.api.base.ResponseModel;
import net.lingmar.web.italker.push.bean.db.User;
import net.lingmar.web.italker.push.factory.UserFactory;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.SubjectSecurityContext;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedAction;

/**
 * 用于所有的请求接口的过滤与拦截
 */
public class AuthRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // 检查是否是登录注册接口
        String relationPath = ((ContainerRequest) requestContext).getPath(false);
        if (relationPath.startsWith("account/login")
                || relationPath.startsWith("account/register")) {
            return;
        }

        // 从Headers中去找到第一个token节点
        String token = requestContext.getHeaders().getFirst("token");
        if (!Strings.isNullOrEmpty(token)) {
            // 查询自己的信息
            User self = UserFactory.findByToken(token);
            if (self != null) {
                // 给当前请求添加一个SecurityContext上下文
                requestContext.setSecurityContext(new SecurityContext() {
                    // 主体部分
                    @Override
                    public Principal getUserPrincipal() {
                        return self;
                    }

                    @Override
                    public boolean isUserInRole(String role) {
                        // 可以在这里写入用户的权限， role是权限名
                        // 可以管理管理员权限等
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        // 默认false即可，检查HTTPS
                        return false;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return null;
                    }
                });
                // 写入上下文后返回
                return ;
            }
        }

        // 直接返回账户需要登录的Model
        ResponseModel model = ResponseModel.buildAccountError();
        // 构建一个返回
        Response response = Response.status(Response.Status.OK)
                .entity(model)
                .build();
        // 拦截
        // 停止一个请求的继续下发，调用该方法后直接返回请求
        // 不会走到Service中去
        requestContext.abortWith(response);
    }
}
