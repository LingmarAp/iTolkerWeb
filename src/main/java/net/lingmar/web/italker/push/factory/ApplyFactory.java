package net.lingmar.web.italker.push.factory;

import net.lingmar.web.italker.push.bean.api.base.JoinModel;
import net.lingmar.web.italker.push.bean.db.Apply;
import net.lingmar.web.italker.push.bean.db.User;
import net.lingmar.web.italker.push.utils.Hib;

public class ApplyFactory {

    // 保存用户的申请
    public static Apply save(JoinModel model, User applicant, String targetId, int type) {
        return Hib.query(session -> {
            Apply apply = new Apply(model,
                    applicant,
                    targetId,
                    type);
            session.save(apply);

            return apply;
        });
    }
}
