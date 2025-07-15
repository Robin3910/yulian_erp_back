package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.Data;
import java.util.Date;

@Data
public class TemuOpenapiShopDO {
    private Integer id;
    private Long tenantId;
    private String shopName;
    private String platform;
    private String shopId;
    private String token;
    private String owner;
    private String appKey;
    private String appSecret;
    private Date authTime;
    private Date authExpireTime;
    private Boolean semiManagedMall;
    private Boolean isThriftStore;
    private Date updateTime;
} 