package net.lingmar.web.italker.push.bean.api.base;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;

public class JoinModel {
    @Expose
    private String description;
    @Expose
    private String attach;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAttach() {
        return attach;
    }

    public void setAttach(String attach) {
        this.attach = attach;
    }

    public static boolean check(JoinModel model) {
        return !Strings.isNullOrEmpty(model.description);
    }
}
