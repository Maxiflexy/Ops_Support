package messaging;

import util.BaseBean;

import java.util.List;

public interface IConvert {
     boolean convertListToJsonString(String status, List<BaseBean> result, BaseBean requestBean, boolean unApproved, boolean approved, boolean unapprovedById);

}