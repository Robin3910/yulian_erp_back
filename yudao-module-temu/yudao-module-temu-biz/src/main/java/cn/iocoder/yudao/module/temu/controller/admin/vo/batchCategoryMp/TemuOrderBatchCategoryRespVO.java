package cn.iocoder.yudao.module.temu.controller.admin.vo.batchCategoryMp;


import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TemuOrderBatchCategoryRespVO {
    private Long id;
    private String batchCategoryId;
    private Long categoryId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String creator;
    private String updater;
    private Boolean deleted;
    private Long tenantId;
}
