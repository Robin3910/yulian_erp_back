package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuOpenapiShopPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuOpenapiShopPageRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOpenapiShopDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TemuOpenapiShopMapper {
    @Insert("INSERT INTO temu_openapi_shop (tenant_id, shop_name, platform, shop_id, token, owner, app_key, app_secret, auth_time, auth_expire_time, semi_managed_mall, is_thrift_store, update_time) " +
            "VALUES (#{tenantId}, #{shopName}, #{platform}, #{shopId}, #{token}, #{owner}, #{appKey}, #{appSecret}, #{authTime}, #{authExpireTime}, #{semiManagedMall}, #{isThriftStore}, #{updateTime})")
    int insert(TemuOpenapiShopDO shopDO);

    @Select("SELECT * FROM temu_openapi_shop WHERE shop_id = #{shopId} LIMIT 1")
    TemuOpenapiShopDO selectByShopId(String shopId);

    /**
     * 分页查询店铺列表
     */
    default PageResult<TemuOpenapiShopPageRespVO> selectPage(TemuOpenapiShopPageReqVO reqVO) {
        // 计算偏移量
        int offset = (reqVO.getPageNo() - 1) * reqVO.getPageSize();
        int limit = reqVO.getPageSize();
        
        // 查询列表
        List<TemuOpenapiShopPageRespVO> list = selectPageList(reqVO, offset, limit);
        // 查询总数
        Long total = selectPageCount(reqVO);
        
        return new PageResult<>(list, total);
    }

    /**
     * 分页查询店铺列表
     */
    @Select("SELECT id, tenant_id as tenantId, shop_name as shopName, platform, shop_id as shopId, token, owner, " +
            "auth_time as authTime, auth_expire_time as authExpireTime, semi_managed_mall as semiManagedMall, " +
            "is_thrift_store as isThriftStore, update_time as updateTime, app_key as appKey, app_secret as appSecret " +
            "FROM temu_openapi_shop " +
            "WHERE (#{reqVO.shopName} IS NULL OR #{reqVO.shopName} = '' OR shop_name LIKE CONCAT('%', #{reqVO.shopName}, '%')) " +
            "AND (#{reqVO.platform} IS NULL OR #{reqVO.platform} = '' OR platform = #{reqVO.platform}) " +
            "ORDER BY update_time DESC " +
            "LIMIT #{offset}, #{limit}")
    List<TemuOpenapiShopPageRespVO> selectPageList(@Param("reqVO") TemuOpenapiShopPageReqVO reqVO, 
                                                   @Param("offset") Integer offset, 
                                                   @Param("limit") Integer limit);

    /**
     * 查询总数
     */
    @Select("SELECT COUNT(*) FROM temu_openapi_shop " +
            "WHERE (#{reqVO.shopName} IS NULL OR #{reqVO.shopName} = '' OR shop_name LIKE CONCAT('%', #{reqVO.shopName}, '%')) " +
            "AND (#{reqVO.platform} IS NULL OR #{reqVO.platform} = '' OR platform = #{reqVO.platform})")
    Long selectPageCount(@Param("reqVO") TemuOpenapiShopPageReqVO reqVO);

    /**
     * 根据ID查询店铺
     */
    @Select("SELECT app_key as appKey, app_secret as appSecret, token FROM temu_openapi_shop WHERE shop_id = #{shopId}")
    TemuOpenapiShopDO selectById(Long shopId);
}