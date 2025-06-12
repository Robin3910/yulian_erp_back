package cn.iocoder.yudao.module.temu.service.imageSearch;

import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageAddReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchOrderRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchReqVO;
import com.aliyun.imagesearch20201214.models.AddImageResponse;
import com.aliyun.imagesearch20201214.models.SearchImageByPicResponse;
import java.util.List;

/**
 * 图像搜索服务接口
 */
public interface TemuImageSearchService {

    /**
     * 添加图片到图像搜索实例
     * 
     * @param reqVO 添加图片请求
     * @return 添加结果
     */
    AddImageResponse addImage(TemuImageAddReqVO reqVO);

    /**
     * 通过图片搜索相似图片
     * 
     * @param reqVO 搜索请求
     * @return 搜索结果
     */
    SearchImageByPicResponse searchImageByPic(TemuImageSearchReqVO reqVO);

    /**
     * 通过图片搜索相似图片（仅返回最高分的结果）
     * 
     * @param reqVO 搜索请求
     * @return 订单详情列表（包含所有最高分结果对应的订单信息）
     */
    List<TemuImageSearchOrderRespVO> searchImageByPicWithHighestScore(TemuImageSearchReqVO reqVO);
}
