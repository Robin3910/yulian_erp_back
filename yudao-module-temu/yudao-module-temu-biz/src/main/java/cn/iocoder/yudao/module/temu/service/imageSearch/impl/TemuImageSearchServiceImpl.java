package cn.iocoder.yudao.module.temu.service.imageSearch.impl;

import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageAddReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchRespVO;
import cn.iocoder.yudao.module.temu.service.imageSearch.TemuImageSearchService;
import cn.iocoder.yudao.module.temu.config.TemuImageSearchConfig;
import com.aliyun.imagesearch20201214.Client;
import com.aliyun.imagesearch20201214.models.AddImageAdvanceRequest;
import com.aliyun.imagesearch20201214.models.AddImageResponse;
import com.aliyun.imagesearch20201214.models.SearchImageByPicAdvanceRequest;
import com.aliyun.imagesearch20201214.models.SearchImageByPicResponse;
import com.aliyun.imagesearch20201214.models.SearchImageByPicResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TemuImageSearchServiceImpl implements TemuImageSearchService {

    @Resource
    private Client imageSearchClient;

    @Resource
    private TemuImageSearchConfig imageSearchConfig;

    //上传图片到阿里云图库
    @Override
    public AddImageResponse addImage(TemuImageAddReqVO reqVO) {
        try {
            AddImageAdvanceRequest request = new AddImageAdvanceRequest();
            // 设置实例名称
            request.instanceName = imageSearchConfig.getInstanceName();
            // 设置定制sku
            request.productId = reqVO.getProductId();
            // 设置图片名称
            request.picName = reqVO.getPicName();

            // 设置主体识别
            request.crop = true;
            // 设置图片内容
            request.picContentObject = reqVO.getFile().getInputStream();

            // 执行添加请求
            RuntimeOptions runtimeOptions = new RuntimeOptions();
            AddImageResponse response = imageSearchClient.addImageAdvance(request, runtimeOptions);

            // 记录日志
            log.info("[addImage][商品({}) 图片({}) 添加成功][response: {}]",
                    reqVO.getProductId(), reqVO.getPicName(), response.getBody());
            return response;

        } catch (TeaException teaException) {
            log.error("[addImage][商品({}) 图片({}) 添加失败] teaException",
                    reqVO.getProductId(), reqVO.getPicName(), teaException);
            throw new RuntimeException(teaException);
        } catch (Exception e) {
            log.error("[addImage][商品({}) 图片({}) 添加失败]",
                    reqVO.getProductId(), reqVO.getPicName(), e);
            throw new RuntimeException(e);
        }
    }

    //图片搜索阿里云图库（返回最高分结果）
    @Override
    public List<TemuImageSearchRespVO> searchImageByPicWithHighestScore(TemuImageSearchReqVO reqVO) {
        try {
            // 创建新的搜索请求对象（避免修改原始请求参数）
            TemuImageSearchReqVO searchReqVO = new TemuImageSearchReqVO();

            // 复制原始请求参数
            searchReqVO.setFile(reqVO.getFile());  // 设置搜索图片文件（Base64/URL格式）
            searchReqVO.setCategoryId(reqVO.getCategoryId()); // 设置图像库的类目ID（限定搜索范围）
            searchReqVO.setNum(10); // 设置返回相同分数结果数量上限（确保获取所有可能结果）
            searchReqVO.setCrop(reqVO.getCrop()); //是否开启主体识别（true/false）
            searchReqVO.setRegion(reqVO.getRegion()); // 指定图片主体区域坐标（格式：x1,x2,y1,y2）

            // 调用基础搜索方法（执行阿里云图像搜索API）
            SearchImageByPicResponse response = searchImageByPic(searchReqVO);

            // 获取搜索结果列表（阿里云返回的相似商品数据）
            List<SearchImageByPicResponseBody.SearchImageByPicResponseBodyAuctions> auctions = response.getBody()
                    .getAuctions();

            // 处理空结果情况
            if (auctions == null || auctions.isEmpty()) {
                return new ArrayList<>();
            }

            // 遍历结果集计算最高分
            float maxScore = 0;
            for (SearchImageByPicResponseBody.SearchImageByPicResponseBodyAuctions auction : auctions) {
                // 更新当前最高分（Score为阿里云返回的相似度分值，范围0-10）
                if (auction.getScore() > maxScore) {
                    maxScore = auction.getScore();
                }
            }

            // 筛选所有最高分结果
            List<TemuImageSearchRespVO> result = new ArrayList<>();
            for (SearchImageByPicResponseBody.SearchImageByPicResponseBodyAuctions auction : auctions) {
                // 匹配当前结果分值与最高分
                if (auction.getScore() == maxScore) {
                    // 封装返回对象（定制sku+相似度分数）
                    result.add(new TemuImageSearchRespVO(
                            auction.getProductId(),  // 定制sku
                            auction.getScore()));   // 相似度分数
                }
            }

            return result;

        } catch (Exception e) {
            log.error("[searchImageByPicWithHighestScore][类目({}) 搜索失败]",
                    reqVO.getCategoryId(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行阿里云图像搜索（以图搜图）核心方法
     * @param reqVO 搜索请求参数封装对象
     * @return 阿里云API返回的搜索结果响应体
     */
    @Override
    public SearchImageByPicResponse searchImageByPic(TemuImageSearchReqVO reqVO) {
        try {
            // 初始化阿里云图像搜索高级请求对象
            SearchImageByPicAdvanceRequest request = new SearchImageByPicAdvanceRequest();
            // 配置实例名称（从配置中心获取）
            request.instanceName = imageSearchConfig.getInstanceName();

            // 设置搜索类目ID（可选）
            if (reqVO.getCategoryId() != null) {
                request.categoryId = reqVO.getCategoryId();
            }

            // 设置返回结果数量（默认10条）
            request.num = reqVO.getNum() != null ? reqVO.getNum() : 10;

            // 启用主体识别功能（默认开启）
            // 说明：自动裁剪背景，聚焦图片主体提升搜索准确率
            request.crop = reqVO.getCrop() != null ? reqVO.getCrop() : true;

            // 设置主体区域（可选）
            if (reqVO.getRegion() != null) {
                request.region = reqVO.getRegion();
            }

            // 设置图片二进制流（通过文件输入流传输）
            // 注意：使用InputStream避免Base64编码的内存开销
            request.picContentObject = reqVO.getFile().getInputStream();

            // 发起API请求（含运行时配置）
            RuntimeOptions runtimeOptions = new RuntimeOptions();
            SearchImageByPicResponse response = imageSearchClient.searchImageByPicAdvance(request, runtimeOptions);

            // 记录日志
            log.info("[searchImageByPic][类目({}) 搜索成功][response: {}]",
                    reqVO.getCategoryId(), response.getBody());
            return response;

        } catch (TeaException teaException) {
            log.error("[searchImageByPic][类目({}) 搜索失败] teaException",
                    reqVO.getCategoryId(), teaException);
            throw new RuntimeException(teaException);
        } catch (Exception e) {
            log.error("[searchImageByPic][类目({}) 搜索失败]",
                    reqVO.getCategoryId(), e);
            throw new RuntimeException(e);
        }
    }


}