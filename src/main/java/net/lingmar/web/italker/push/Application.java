package net.lingmar.web.italker.push;

import net.lingmar.web.italker.push.provider.AuthRequestFilter;
import net.lingmar.web.italker.push.provider.GsonProvider;
import net.lingmar.web.italker.push.service.AccountService;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.logging.Logger;

public class Application extends ResourceConfig {

    public Application() {
        // 注册逻辑处理的包名
        packages(AccountService.class.getPackage().getName());

        // 注册全局请求拦截器
        register(AuthRequestFilter.class);

        // 注册Json解析器
        // register(JacksonJsonProvider.class);
        register(GsonProvider.class);

        // 注册日志打印输出
        register(Logger.class);

    }
}
