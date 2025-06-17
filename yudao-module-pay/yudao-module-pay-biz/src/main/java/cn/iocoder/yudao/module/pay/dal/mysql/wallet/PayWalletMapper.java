package cn.iocoder.yudao.module.pay.dal.mysql.wallet;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pay.controller.admin.wallet.vo.wallet.PayWalletPageReqVO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayWalletMapper extends BaseMapperX<PayWalletDO> {

    default PayWalletDO selectByUserIdAndType(Long userId, Integer userType) {
        return selectOne(PayWalletDO::getUserId, userId,
                PayWalletDO::getUserType, userType);
    }

    default PageResult<PayWalletDO> selectPage(PayWalletPageReqVO reqVO) {
        // 创建 MPJLambdaWrapper 用于连表查询
        MPJLambdaWrapper<PayWalletDO> wrapper = new MPJLambdaWrapper<PayWalletDO>()
                .selectAll(PayWalletDO.class) // 查询钱包表全部字段
                .select("u.nickname") // 查询用户昵称
                .leftJoin("system_users u on t.user_id = u.id"); // 左连接用户表
                
        // 添加查询条件
        if (reqVO.getUserId() != null) {
            wrapper.eq(PayWalletDO::getUserId, reqVO.getUserId());
        }
        if (reqVO.getUserType() != null) {
            wrapper.eq(PayWalletDO::getUserType, reqVO.getUserType());
        }
        if (reqVO.getCreateTime() != null && reqVO.getCreateTime().length == 2) {
            wrapper.between(PayWalletDO::getCreateTime, 
                    reqVO.getCreateTime()[0], 
                    reqVO.getCreateTime()[1]);
        }
        
        wrapper.orderByDesc(PayWalletDO::getId);
                
        return selectJoinPage(reqVO, PayWalletDO.class, wrapper);
    }

    /**
     * 当消费退款时候， 更新钱包
     *
     * @param id 钱包 id
     * @param price 消费金额
     */
    default int updateWhenConsumptionRefund(Long id, Integer price) {
        LambdaUpdateWrapper<PayWalletDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<PayWalletDO>()
                .setSql(" balance = balance + " + price
                        + ", total_expense = total_expense - " + price)
                .eq(PayWalletDO::getId, id);
        return update(null, lambdaUpdateWrapper);
    }

    /**
     * 当消费时候， 更新钱包
     *
     * @param price 消费金额
     * @param id 钱包 id
     */
    default int updateWhenConsumption(Long id, Integer price){
        LambdaUpdateWrapper<PayWalletDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<PayWalletDO>()
                .setSql(" balance = balance - " + price
                        + ", total_expense = total_expense + " + price)
                .eq(PayWalletDO::getId, id)
                .ge(PayWalletDO::getBalance, price); // cas 逻辑
        return update(null, lambdaUpdateWrapper);
    }

    /**
     * 当充值的时候，更新钱包
     *
     * @param id 钱包 id
     * @param price 钱包金额
     */
    default int updateWhenRecharge(Long id, Integer price){
        LambdaUpdateWrapper<PayWalletDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<PayWalletDO>()
                .setSql(" balance = balance + " + price
                        + ", total_recharge = total_recharge + " + price)
                .eq(PayWalletDO::getId, id);
        return update(null, lambdaUpdateWrapper);
    }

    /**
     * 增加余额的时候，更新钱包
     *
     * @param id 钱包 id
     * @param price 钱包金额
     */
    default void updateWhenAdd(Long id, Integer price) {
        LambdaUpdateWrapper<PayWalletDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<PayWalletDO>()
             .setSql(" balance = balance + " + price)
             .eq(PayWalletDO::getId, id);
        update(null, lambdaUpdateWrapper);
    }

    /**
     * 冻结钱包部分余额
     *
     * @param id 钱包 id
     * @param price 冻结金额
     */
    default int freezePrice(Long id, Integer price){
        LambdaUpdateWrapper<PayWalletDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<PayWalletDO>()
                .setSql(" balance = balance - " + price
                        + ", freeze_price = freeze_price + " + price)
                .eq(PayWalletDO::getId, id)
                .ge(PayWalletDO::getBalance, price); // cas 逻辑
        return update(null, lambdaUpdateWrapper);
    }

    /**
     * 解冻钱包余额
     *
     * @param id 钱包 id
     * @param price 解冻金额
     */
    default int unFreezePrice(Long id, Integer price){
        LambdaUpdateWrapper<PayWalletDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<PayWalletDO>()
                .setSql(" balance = balance + " + price
                        + ", freeze_price = freeze_price - " + price)
                .eq(PayWalletDO::getId, id)
                .ge(PayWalletDO::getFreezePrice, price); // cas 逻辑
        return update(null, lambdaUpdateWrapper);
    }

    /**
     * 当充值退款时, 更新钱包
     *
     * @param id 钱包 id
     * @param price 退款金额
     */
    default  int updateWhenRechargeRefund(Long id, Integer price){
        LambdaUpdateWrapper<PayWalletDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<PayWalletDO>()
                .setSql(" freeze_price = freeze_price - " + price
                        + ", total_recharge = total_recharge - " + price)
                .eq(PayWalletDO::getId, id)
                .ge(PayWalletDO::getFreezePrice, price)
                .ge(PayWalletDO::getTotalRecharge, price);// cas 逻辑
        return update(null, lambdaUpdateWrapper);
    }

}




