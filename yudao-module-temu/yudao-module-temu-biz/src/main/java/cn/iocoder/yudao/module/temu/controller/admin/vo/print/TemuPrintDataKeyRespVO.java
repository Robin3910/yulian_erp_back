package cn.iocoder.yudao.module.temu.controller.admin.vo.print;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "TemuApi - 打印数据Key Response VO")
@Data
public class TemuPrintDataKeyRespVO {

    @Schema(description = "打印数据Key")
    private String dataKey;

    @Schema(description = "打印页面URL")
    private String printUrl;

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
        if (dataKey != null && !dataKey.isEmpty()) {
            this.printUrl = "https://openapi.kuajingmaihuo.com/tool/print?dataKey=" + dataKey;
        }
    }
}