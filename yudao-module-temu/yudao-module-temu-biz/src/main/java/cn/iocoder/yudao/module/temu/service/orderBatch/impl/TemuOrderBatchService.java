package cn.iocoder.yudao.module.temu.service.orderBatch.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.*;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;
import cn.iocoder.yudao.module.temu.dal.mysql.*;
import cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.temu.enums.TemuOrderBatchStatusEnum;
import cn.iocoder.yudao.module.temu.enums.TemuOrderStatusEnum;
import cn.iocoder.yudao.module.temu.service.orderBatch.ITemuOrderBatchService;
import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.temu.enums.TemuOrderBatchStatusEnum.*;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TemuOrderBatchService implements ITemuOrderBatchService {
	
	@Resource
	private TemuOrderBatchMapper temuOrderBatchMapper;
	@Resource
	private TemuOrderMapper temuOrderMapper;
	@Resource
	private TemuOrderBatchRelationMapper temuOrderBatchRelationMapper;
	@Resource
	private TemuShopMapper temuShopMapper;
	@Resource
	private AdminUserMapper adminUserMapper;
	@Resource
	private OrderBatchTaskMapper orderBatchTaskMapper;
	@Resource
	private WeiXinProducer weiXinProducer;
	
	/*
	 * 创建批次订单
	 * @param temuOrderBatchCreateVO 批次订单创建VO
	 * @return 批次订单关系记录数量
	 */
	@Override
	@Transactional
	public Integer createBatch(TemuOrderBatchCreateVO temuOrderBatchCreateVO) {
		if (temuOrderBatchCreateVO == null || temuOrderBatchCreateVO.getOrderIds().isEmpty()) {
			throw exception(ORDER_NOT_EXISTS);
		}
		//数据去重
		List<Long> orderIds = temuOrderBatchCreateVO.getOrderIds().stream().distinct().collect(Collectors.toList());
		
		List<TemuOrderBatchRelationDO> temuOrderBatchRelationDOList = CollectionUtils.convertList(orderIds, orderId -> {
			TemuOrderBatchRelationDO temuOrderBatchRelationDO = new TemuOrderBatchRelationDO();
			temuOrderBatchRelationDO.setOrderId(orderId);
			return temuOrderBatchRelationDO;
		});
		
		// 检查订单是否存在，并保存订单信息用于后续统计
		List<TemuOrderDO> orders = new ArrayList<>();
		temuOrderBatchRelationDOList.forEach(temuOrderBatchRelationDOItem -> {
			TemuOrderDO temuOrderDO = temuOrderMapper.selectById(temuOrderBatchRelationDOItem.getOrderId());
			// 检查订单是否存在 
			if (temuOrderDO == null) {
				throw exception(ORDER_NOT_EXISTS);
			}
			//检查订单是否已经被下单
			if (temuOrderDO.getOrderStatus() != TemuOrderStatusEnum.ORDERED) {
				throw exception(ORDER_STATUS_ERROR);
			}
			//检查订单是否已经被批次化
			// 检查订单状态是否存在历史批次中
			Long count = temuOrderBatchRelationMapper.selectCount("order_id", temuOrderBatchRelationDOItem.getOrderId());
			if (count > 0) {
				throw exception(ORDER_BATCH_EXISTS);
			}
			orders.add(temuOrderDO);
		});
		
		//批量更新订单
		QueryWrapper<TemuOrderDO> temuOrderDOQueryWrapper = new QueryWrapper<>();
		temuOrderDOQueryWrapper.in("id", orderIds);
		TemuOrderDO temuOrderDO = new TemuOrderDO();
		//更新订单状态 已送产待生产
		temuOrderDO.setOrderStatus(TemuOrderStatusEnum.IN_PRODUCTION);
		int update = temuOrderMapper.update(temuOrderDO, temuOrderDOQueryWrapper);
		if (update <= 0) {
			throw exception(ORDER_BATCH_CREATE_FAIL);
		}
		//插入批次
		//创建批次订单记录
		TemuOrderBatchDO temuOrderBatchDO = new TemuOrderBatchDO();
		//设置uuid年月日时分秒毫秒
		DateTime dateTime = DateTime.now();
		temuOrderBatchDO.setBatchNo(dateTime.toString("yyyyMMddHHmmss"));
		int insert = temuOrderBatchMapper.insert(temuOrderBatchDO);
		if (insert <= 0) {
			throw exception(ORDER_BATCH_CREATE_FAIL);
		}
		temuOrderBatchRelationDOList.forEach(temuOrderBatchRelationDOItem -> {
			temuOrderBatchRelationDOItem.setBatchId(temuOrderBatchDO.getId());
		});
		Boolean result = temuOrderBatchRelationMapper.insertBatch(temuOrderBatchRelationDOList);
		
		// 发送webhook通知
		if (result) {
			// 获取所有类目
			Set<String> categories = orders.stream()
					.map(TemuOrderDO::getCategoryName)
					.collect(Collectors.toSet());
			
			// 构建通知消息
			StringBuilder message = new StringBuilder();
			message.append("批次打包完成通知\n");
			message.append("批次编号：").append(temuOrderBatchDO.getBatchNo()).append("\n");
			message.append("订单数量：").append(orders.size()).append("\n");
			message.append("类目列表：\n");
			categories.forEach(category ->
					message.append("- ").append(category).append("\n")
			);
			
			// 获取shopId为88888888的店铺webhook地址
			TemuShopDO shop = temuShopMapper.selectByShopId(88888888L);
			if (shop != null && StringUtils.isNotEmpty(shop.getWebhook())) {
				weiXinProducer.sendMessage(shop.getWebhook(), message.toString());
			}
		}
		
		return result ? temuOrderBatchRelationDOList.size() : null;
	}
	
	/*
	 * 分页查询批次订单
	 * @param temuOrderBatchPageVO 批次订单分页查询VO
	 * @return 批次订单分页查询结果
	 */
	@Override
	public PageResult<TemuOrderBatchDetailDO> list(TemuOrderBatchPageVO temuOrderBatchPageVO) {
		long startTime = System.currentTimeMillis();
		log.info("开始查询批次ID");
		
		// 第一步：查询满足条件的批次ID
		PageResult<TemuOrderBatchDO> batchPageResult = temuOrderBatchMapper.selectBatchPage(temuOrderBatchPageVO);
		if (batchPageResult.getList().isEmpty()) {
			log.info("批次查询耗时：{}ms", System.currentTimeMillis() - startTime);
			return new PageResult<>(new ArrayList<>(), batchPageResult.getTotal(), temuOrderBatchPageVO.getPageNo(),
					temuOrderBatchPageVO.getPageSize());
		}
		
		long step1Time = System.currentTimeMillis();
		log.info("查询批次ID耗时：{}ms", step1Time - startTime);
		log.info("开始查询批次详细信息");
		
		// 第二步：查询批次详细信息和关联的所有订单
		List<Long> batchIds = batchPageResult.getList().stream()
				.map(TemuOrderBatchDO::getId)
				.collect(Collectors.toList());
		
		// 查询批次基本信息
		List<TemuOrderBatchDO> batchList = temuOrderBatchMapper.selectBatchList(batchIds);
		long batchDetailTime = System.currentTimeMillis();
		log.info("查询批次基本信息耗时：{}ms", batchDetailTime - step1Time);

		// 先查询relation表中的orderId
		MPJLambdaWrapperX<TemuOrderBatchRelationDO> relationWrapper = new MPJLambdaWrapperX<>();
		relationWrapper.select(TemuOrderBatchRelationDO::getBatchId, TemuOrderBatchRelationDO::getOrderId)
				.in(TemuOrderBatchRelationDO::getBatchId, batchIds);
		List<TemuOrderBatchRelationDO> relationList = temuOrderBatchRelationMapper.selectJoinList(TemuOrderBatchRelationDO.class, relationWrapper);
		
		// log.info("查询到的relationList大小: {}", relationList != null ? relationList.size() : 0);
		// if (relationList != null && !relationList.isEmpty()) {
		// 	log.info("relationList第一条数据: batchId={}, orderId={}", 
		// 		relationList.get(0).getBatchId(), 
		// 		relationList.get(0).getOrderId());
		// }
		
		// 获取所有orderId
		List<Long> orderIds = relationList.stream()
				.map(TemuOrderBatchRelationDO::getOrderId)
				.collect(Collectors.toList());
		
		// log.info("提取的orderIds大小: {}", orderIds.size());
		
		// 根据orderId查询订单信息
		MPJLambdaWrapperX<TemuOrderDO> orderWrapper = new MPJLambdaWrapperX<>();
		// price_rule不能查，price_rule出错了，数据量巨大
		orderWrapper.select("id, order_no, product_title, order_status, sku, skc, sale_price, custom_sku, " +
				"quantity, product_properties, booking_time, shop_id, create_time, update_time, custom_image_urls, " +
				"custom_text_list, product_img_url, category_id, category_name, " +
				"effective_img_url, unit_price, total_price, default_price, " +
				"goods_sn, compliance_url, remark, original_quantity, compliance_image_url, compliance_goods_merged_url, " +
				"is_complete_draw_task, is_complete_producer_task")
				.in(TemuOrderDO::getId, orderIds)
				.eq(StringUtils.isNotEmpty(temuOrderBatchPageVO.getCustomSku()), TemuOrderDO::getCustomSku, temuOrderBatchPageVO.getCustomSku())
				.orderByDesc(TemuOrderDO::getCreateTime);
		
		List<TemuOrderDO> orderList = temuOrderMapper.selectJoinList(TemuOrderDO.class, orderWrapper);
		// log.info("查询到的orderList大小: {}", orderList != null ? orderList.size() : 0);
		
		// 构建orderId到订单的映射
		Map<Long, TemuOrderDO> orderMap = orderList.stream()
				.filter(order -> order != null && order.getId() != null)
				.collect(Collectors.toMap(TemuOrderDO::getId, order -> order, (existing, replacement) -> existing));
		
		// log.info("构建的orderMap大小: {}", orderMap.size());
		
		// 构建batchId到orderId列表的映射
		Map<Long, List<Long>> batchOrderMap = relationList.stream()
				.filter(relation -> relation != null && relation.getBatchId() != null && relation.getOrderId() != null)
				.collect(Collectors.groupingBy(
						TemuOrderBatchRelationDO::getBatchId,
						Collectors.mapping(TemuOrderBatchRelationDO::getOrderId, Collectors.toList())
				));
		
		// log.info("构建的batchOrderMap大小: {}", batchOrderMap.size());
		// if (!batchOrderMap.isEmpty()) {
		// 	log.info("batchOrderMap第一个key: {}, 对应的orderIds: {}", 
		// 		batchOrderMap.keySet().iterator().next(),
		// 		batchOrderMap.values().iterator().next());
		// }
		
		// 组装最终结果
		List<TemuOrderBatchDetailDO> resultList = new ArrayList<>();
		for (TemuOrderBatchDO batch : batchList) {
			if (batch == null) {
				continue;
			}
			TemuOrderBatchDetailDO detail = new TemuOrderBatchDetailDO();
			BeanUtils.copyProperties(batch, detail);
			
			List<Long> batchOrderIds = batchOrderMap.get(batch.getId());
			if (batchOrderIds != null && !batchOrderIds.isEmpty()) {
				List<TemuOrderDetailDO> orders = batchOrderIds.stream()
						.map(orderMap::get)
						.filter(Objects::nonNull)
						.map(order -> {
							TemuOrderDetailDO orderDetail = new TemuOrderDetailDO();
							BeanUtils.copyProperties(order, orderDetail);
							return orderDetail;
						})
						.collect(Collectors.toList());
				detail.setOrderList(orders);
			} else {
				detail.setOrderList(new ArrayList<>());
			}
			resultList.add(detail);
		}
		
		long step2Time = System.currentTimeMillis();
		log.info("查询批次详细信息耗时：{}ms", step2Time - batchDetailTime);
		log.info("开始处理结果数据");
		
		// 获取所有分配关联用户的信息
		resultList.forEach(batch -> {
			MPJLambdaWrapperX<TemuOrderBatchTaskDO> objectMPJLambdaWrapperX = new MPJLambdaWrapperX<>();
			objectMPJLambdaWrapperX.selectAll(TemuOrderBatchTaskDO.class)
					.selectAs(AdminUserDO::getId, TemuOrderBatchTaskUserInfoDO::getUserId)
					.selectAs(AdminUserDO::getNickname, TemuOrderBatchTaskUserInfoDO::getNickName)
					.leftJoin(AdminUserDO.class, AdminUserDO::getId, TemuOrderBatchTaskDO::getUserId)
					.eq(TemuOrderBatchTaskDO::getBatchOrderId, batch.getId());
			List<TemuOrderBatchTaskUserInfoDO> adminUserDOS = orderBatchTaskMapper.selectJoinList(TemuOrderBatchTaskUserInfoDO.class, objectMPJLambdaWrapperX);
			batch.setUserList(adminUserDOS);
		});
		
		// 获取所有订单的shopId
		List<Long> shopIds = resultList.stream()
				.flatMap(batch -> batch.getOrderList().stream())
				.map(TemuOrderDetailDO::getShopId)
				.distinct()
				.collect(Collectors.toList());
		
		// 批量查询店铺信息
		if (!shopIds.isEmpty()) {
			Map<Long, TemuShopDO> shopMap = temuShopMapper.selectList(new QueryWrapper<TemuShopDO>()
							.in("shop_id", shopIds))
					.stream()
					.collect(Collectors.toMap(TemuShopDO::getShopId, shop -> shop));
			
			// 为每个订单设置shopName
			resultList.forEach(batch -> {
				if (batch.getOrderList() != null) {
					batch.getOrderList().forEach(order -> {
						TemuShopDO shop = shopMap.get(order.getShopId());
						if (shop != null) {
							order.setShopName(shop.getShopName());
						}
					});
				}
			});
		}
		
		return new PageResult<>(resultList, batchPageResult.getTotal(), temuOrderBatchPageVO.getPageNo(), temuOrderBatchPageVO.getPageSize());
	}
	
	@Override
	public PageResult<TemuOrderBatchUserDetailDO> taskList(TemuOrderBatchPageVO temuOrderBatchPageVO) {
		PageResult<TemuOrderBatchUserDetailDO> temuOrderBatchDOPageResult = selectUserPage(temuOrderBatchPageVO);
		// 获取所有分配关联用户的信息
		temuOrderBatchDOPageResult.getList().forEach(batch -> {
			MPJLambdaWrapperX<TemuOrderBatchTaskDO> objectMPJLambdaWrapperX = new MPJLambdaWrapperX<>();
			objectMPJLambdaWrapperX.selectAll(TemuOrderBatchTaskDO.class)
					.selectAs(AdminUserDO::getId, TemuOrderBatchTaskUserInfoDO::getUserId)
					.selectAs(AdminUserDO::getNickname, TemuOrderBatchTaskUserInfoDO::getNickName)
					.leftJoin(AdminUserDO.class, AdminUserDO::getId, TemuOrderBatchTaskDO::getUserId)
					.eq(TemuOrderBatchTaskDO::getBatchOrderId, batch.getId());
			List<TemuOrderBatchTaskUserInfoDO> adminUserDOS = orderBatchTaskMapper.selectJoinList(TemuOrderBatchTaskUserInfoDO.class, objectMPJLambdaWrapperX);
			batch.setUserList(adminUserDOS);
		});
		// 获取所有订单的shopId
		List<Long> shopIds = temuOrderBatchDOPageResult.getList().stream()
				.flatMap(batch -> batch.getOrderList().stream())
				.map(TemuOrderDetailDO::getShopId)
				.distinct()
				.collect(Collectors.toList());
		
		// 批量查询店铺信息
		if (!shopIds.isEmpty()) {
			Map<Long, TemuShopDO> shopMap = temuShopMapper.selectList(new QueryWrapper<TemuShopDO>()
							.in("shop_id", shopIds))
					.stream()
					.collect(Collectors.toMap(TemuShopDO::getShopId, shop -> shop));
			
			// 为每个订单设置shopName
			temuOrderBatchDOPageResult.getList().forEach(batch -> {
				if (batch.getOrderList() != null) {
					batch.getOrderList().forEach(order -> {
						TemuShopDO shop = shopMap.get(order.getShopId());
						if (shop != null) {
							order.setShopName(shop.getShopName());
						}
					});
				}
			});
		}
		
		return temuOrderBatchDOPageResult;
	}
	
	/**
	 * 更新批次文件信息
	 * 该方法通过接收一个 TemuOrderBatchUpdateFileVO 对象作为参数，将其转换为 TemuOrderBatchDO 对象，并调用 mapper 层的更新方法
	 * 主要目的是为了更新数据库中批次文件的相关信息
	 *
	 * @param temuOrderBatchUpdateFileVO 包含要更新的批次文件信息的视图对象
	 * @return 返回更新操作影响的行数
	 */
	@Override
	public int updateBatchFile(TemuOrderBatchUpdateFileVO temuOrderBatchUpdateFileVO) {
		return temuOrderBatchMapper.updateById(BeanUtils.toBean(temuOrderBatchUpdateFileVO, TemuOrderBatchDO.class));
	}
	
	@Override
	@Transactional
	public int updateBatchFileByTask(TemuOrderBatchUpdateFileByTaskVO temuOrderBatchUpdateFileVO) {
		//检查批次任务状态
		TemuOrderBatchTaskDO temuOrderBatchTaskDO = orderBatchTaskMapper.selectById(temuOrderBatchUpdateFileVO.getTaskId());
		if (temuOrderBatchTaskDO == null) {
			throw exception(ORDER_BATCH_TASK_NOT_EXISTS);
		}
		//检查当前任务是否由处理人处理
		if (!Objects.equals(temuOrderBatchTaskDO.getUserId(), SecurityFrameworkUtils.getLoginUserId())) {
			throw exception(ORDER_BATCH_TASK_NOT_OWNER);
		}
		//待处理状态下完成任务 ，并且修改下个关联任务的状态
		//if (temuOrderBatchTaskDO.getStatus() == TemuOrderBatchStatusEnum.TASK_STATUS_WAIT) {
		//	temuOrderBatchTaskDO.setStatus(TemuOrderBatchStatusEnum.TASK_STATUS_COMPLETE);
		//	orderBatchTaskMapper.updateById(temuOrderBatchTaskDO);
		//	if (temuOrderBatchTaskDO.getNextTaskType() != null) {
		//		//	查找类型和相同批次id任务
		//		TemuOrderBatchTaskDO nextBatchTaskDO = orderBatchTaskMapper.selectOne(new QueryWrapper<TemuOrderBatchTaskDO>()
		//				.eq("batch_order_id", temuOrderBatchTaskDO.getBatchOrderId())
		//				.eq("type", temuOrderBatchTaskDO.getNextTaskType())
		//				.eq("status", TemuOrderBatchStatusEnum.TASK_STATUS_NOT_HANDLED)
		//		);
		//		if (nextBatchTaskDO != null) {
		//			nextBatchTaskDO.setStatus(TemuOrderBatchStatusEnum.TASK_STATUS_WAIT);
		//			orderBatchTaskMapper.updateById(nextBatchTaskDO);
		//		}
		//	}
		//}
		return temuOrderBatchMapper.updateById(BeanUtils.toBean(temuOrderBatchUpdateFileVO, TemuOrderBatchDO.class));
	}
	
	@Override
	@Transactional
	public int updateStatus(TemuOrderBatchUpdateStatusVO temuOrderBatchUpdateStatusVO) {
		//检查批次订单是否存在
		TemuOrderBatchDO temuOrderBatchDO = temuOrderBatchMapper.selectById(temuOrderBatchUpdateStatusVO.getId());
		if (temuOrderBatchDO == null) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		if (temuOrderBatchDO.getStatus() != TemuOrderBatchStatusEnum.IN_PRODUCTION) {
			throw exception(ORDER_BATCH_STATUS_ERROR);
		}
		List<TemuOrderBatchRelationDO> temuOrderBatchRelationDOList = temuOrderBatchRelationMapper.selectByMap(MapUtil.<String, Object>builder()
				.put("batch_id", temuOrderBatchUpdateStatusVO.getId())
				.build()
		);
		if (temuOrderBatchRelationDOList == null || temuOrderBatchRelationDOList.isEmpty()) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		//获取所有的订单id
		List<Long> orderIds = temuOrderBatchRelationDOList.stream().map(TemuOrderBatchRelationDO::getOrderId).collect(Collectors.toList());
		//批量更新订单状态
		List<TemuOrderDO> temuOrderDOList = orderIds.stream().unordered().map(orderId -> {
			TemuOrderDO temuOrderDO = new TemuOrderDO();
			temuOrderDO.setId(orderId);
			temuOrderDO.setOrderStatus(TemuOrderStatusEnum.SHIPPED);
			temuOrderDO.setIsCompleteDrawTask(ORDER_TASK_STATUS_COMPLETE);
			temuOrderDO.setIsCompleteProducerTask(ORDER_TASK_STATUS_COMPLETE);
			return temuOrderDO;
		}).collect(Collectors.toList());
		temuOrderMapper.updateBatch(temuOrderDOList);
		//设置批次订单状态
		temuOrderBatchDO.setStatus(TemuOrderBatchStatusEnum.PRODUCTION_COMPLETE);
		return temuOrderBatchMapper.updateById(temuOrderBatchDO);
	}
	
	@Override
	@Transactional
	public int updateStatusByTask(TemuOrderBatchUpdateStatusByTaskVO temuOrderBatchUpdateStatusVO) {
		//检查批次任务状态
		TemuOrderBatchTaskDO temuOrderBatchTaskDO = orderBatchTaskMapper.selectById(temuOrderBatchUpdateStatusVO.getTaskId());
		if (temuOrderBatchTaskDO == null) {
			throw exception(ORDER_BATCH_TASK_NOT_EXISTS);
		}
		//检查当前任务是否由处理人处理
		if (!Objects.equals(temuOrderBatchTaskDO.getUserId(), SecurityFrameworkUtils.getLoginUserId())) {
			throw exception(ORDER_BATCH_TASK_NOT_OWNER);
		}
		if (temuOrderBatchTaskDO.getStatus() == TemuOrderBatchStatusEnum.TASK_STATUS_NOT_HANDLED) {
			throw exception(ORDER_STATUS_ERROR);
		}
		// 检查任务是否已经完成
		if (temuOrderBatchTaskDO.getStatus() == TemuOrderBatchStatusEnum.TASK_STATUS_COMPLETE) {
			throw exception(ORDER_BATCH_TASK_COMPLETE);
		}
		
		//检查关联的批次订单是否存在
		TemuOrderBatchDO temuOrderBatchDO = temuOrderBatchMapper.selectById(temuOrderBatchUpdateStatusVO.getId());
		if (temuOrderBatchDO == null) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		if (temuOrderBatchDO.getStatus() != TemuOrderBatchStatusEnum.IN_PRODUCTION) {
			throw exception(ORDER_BATCH_STATUS_ERROR);
		}
		List<TemuOrderBatchRelationDO> temuOrderBatchRelationDOList = temuOrderBatchRelationMapper.selectByMap(MapUtil.<String, Object>builder()
				.put("batch_id", temuOrderBatchUpdateStatusVO.getId())
				.build()
		);
		if (temuOrderBatchRelationDOList == null || temuOrderBatchRelationDOList.isEmpty()) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		
		//获取所有的订单id
		List<Long> orderIds = temuOrderBatchRelationDOList.stream().map(TemuOrderBatchRelationDO::getOrderId).collect(Collectors.toList());
		//批量更新订单状态
		List<TemuOrderDO> temuOrderDOList = orderIds.stream().unordered().map(orderId -> {
			TemuOrderDO temuOrderDO = new TemuOrderDO();
			temuOrderDO.setId(orderId);
			//作图员
			if (temuOrderBatchTaskDO.getType() == TASK_TYPE_PRODUCTION) {
				temuOrderDO.setOrderStatus(TemuOrderStatusEnum.SHIPPED);
				temuOrderDO.setIsCompleteProducerTask(1);
				
			}
			//生产员
			if (temuOrderBatchTaskDO.getType() == TASK_TYPE_ART) {
				temuOrderDO.setIsCompleteDrawTask(TemuOrderStatusEnum.TASK_STATUS_COMPLETE);
			}
			return temuOrderDO;
		}).collect(Collectors.toList());
		temuOrderMapper.updateBatch(temuOrderDOList);
		//设置批次订单状态 生产任务完成 则整个任务就完成了
		if (temuOrderBatchTaskDO.getType() == TASK_TYPE_PRODUCTION) {
			temuOrderBatchDO.setStatus(TemuOrderBatchStatusEnum.PRODUCTION_COMPLETE);
			temuOrderBatchMapper.updateById(temuOrderBatchDO);
		}
		
		//设置已分配任务的完成状态
		temuOrderBatchTaskDO.setStatus(TASK_STATUS_COMPLETE);
		
		return orderBatchTaskMapper.updateById(temuOrderBatchTaskDO);
	}
	
	@Override
	public Boolean saveOrderRemark(TemuOrderBatchSaveOrderRemarkReqVO requestVO) {
		//检查批次订单是否存在
		TemuOrderBatchDO temuOrderBatchDO = temuOrderBatchMapper.selectById(requestVO.getOrderId());
		if (temuOrderBatchDO == null) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		temuOrderBatchDO.setRemark(requestVO.getRemark());
		
		return temuOrderBatchMapper.updateById(temuOrderBatchDO) > 0;
	}
	
	@Override
	@Transactional
	public Boolean dispatchTask(TemuOrderBatchDispatchTaskVO requestVO) {
		//检查批次订单是否存在
		if (requestVO.getOrderIds() == null || requestVO.getOrderIds().length == 0) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		//检查用户是否存在
		AdminUserDO artStaffUserDO = adminUserMapper.selectById(requestVO.getArtStaffUserId());
		if (artStaffUserDO == null) {
			throw exception(USER_NOT_EXISTS);
		}
		AdminUserDO productionStaffUserDO = adminUserMapper.selectById(requestVO.getProductionStaffUserId());
		if (productionStaffUserDO == null) {
			throw exception(USER_NOT_EXISTS);
		}
		List<TemuOrderBatchTaskDO> temuOrderBatchTaskDOList = new ArrayList<>();
		List<TemuOrderBatchDO> temuOrderBatchDOList = new ArrayList<>();
		//检查批次订单状态是否待生产 未分配
		for (Long orderId : requestVO.getOrderIds()) {
			TemuOrderBatchDO temuOrderBatchDO = temuOrderBatchMapper.selectById(orderId);
			if (temuOrderBatchDO == null) {
				throw exception(ORDER_BATCH_NOT_EXISTS);
			}
			if (temuOrderBatchDO.getStatus() != TemuOrderBatchStatusEnum.IN_PRODUCTION) {
				throw exception(ORDER_BATCH_STATUS_ERROR);
			}
			if (temuOrderBatchDO.getIsDispatchTask() != null && temuOrderBatchDO.getIsDispatchTask() == DISPATCH_TASK) {
				throw exception(ORDER_BATCH_STATUS_ERROR);
			}
			//批量修改批次下订单的任务状态
			List<TemuOrderBatchRelationDO> temuOrderBatchRelationList = temuOrderBatchRelationMapper.selectList(TemuOrderBatchRelationDO::getBatchId, orderId);
			if (temuOrderBatchRelationList != null && !temuOrderBatchRelationList.isEmpty()) {
				List<TemuOrderDO> collect = temuOrderBatchRelationList.stream().map(temuOrderBatchRelationDO -> {
					TemuOrderDO temuOrderDO = new TemuOrderDO();
					temuOrderDO.setId(temuOrderBatchRelationDO.getOrderId());
					temuOrderDO.setIsCompleteDrawTask(TemuOrderStatusEnum.TASK_STATUS_WAIT);
					temuOrderDO.setIsCompleteProducerTask(TemuOrderStatusEnum.TASK_STATUS_WAIT);
					return temuOrderDO;
				}).collect(Collectors.toList());
				//初始化任务完成状态
				temuOrderMapper.updateBatch(collect);
			}
			//设置批次分配状态
			temuOrderBatchDO.setIsDispatchTask(DISPATCH_TASK);
			temuOrderBatchDOList.add(temuOrderBatchDO);
			//作图员任务分配
			temuOrderBatchTaskDOList.add(TemuOrderBatchTaskDO.builder()
					.batchOrderId(orderId)
					.userId(requestVO.getArtStaffUserId())
					.type(TASK_TYPE_ART)
					.status(TASK_STATUS_WAIT)
					.nextTaskType(TASK_TYPE_PRODUCTION)
					.build());
			//生产员任务分配
			temuOrderBatchTaskDOList.add(TemuOrderBatchTaskDO.builder()
					.batchOrderId(orderId)
					.userId(requestVO.getProductionStaffUserId())
					.status(TASK_STATUS_WAIT)
					.type(TASK_TYPE_PRODUCTION)
					.build());
		}
		//处理批量插入
		temuOrderBatchMapper.updateBatch(temuOrderBatchDOList);
		orderBatchTaskMapper.insertBatch(temuOrderBatchTaskDOList);
		return true;
	}
	
	@Override
	@Transactional
	public int completeBatchOrderTask(TemuOrderBatchCompleteOrderTaskVO requestVO) {
		//检查批次任务是否存在
		TemuOrderBatchTaskDO orderBatchTaskDO = orderBatchTaskMapper.selectById(requestVO.getTaskId());
		if (orderBatchTaskDO == null) {
			throw exception(ErrorCodeConstants.ORDER_BATCH_TASK_NOT_EXISTS);
		}
		//检查批次任务是否已经完成
		if (orderBatchTaskDO.getStatus() != TASK_STATUS_WAIT) {
			throw exception(ErrorCodeConstants.ORDER_BATCH_TASK_STATUS_ERROR);
		}
		//批次根据批次类型设置订单的状态
		TemuOrderDO temuOrderDO = new TemuOrderDO();
		temuOrderDO.setId(requestVO.getOrderId());
		switch (orderBatchTaskDO.getType()) {
			case TASK_TYPE_ART:
				//作图任务
				temuOrderDO.setIsCompleteDrawTask(ORDER_TASK_STATUS_COMPLETE);
				break;
			case TASK_TYPE_PRODUCTION:
				if(temuOrderDO.getIsCompleteDrawTask()!=ORDER_TASK_STATUS_COMPLETE){
					throw exception(ErrorCodeConstants.ORDER_BATCH_TASK_DRAW_NOT_COMPLETE);
				}
				//生产任务
				temuOrderDO.setIsCompleteProducerTask(ORDER_TASK_STATUS_COMPLETE);
				break;
		}
		temuOrderMapper.updateById(temuOrderDO);
		//检查当前任务关联的订单是否完成当前类型的所有任务  如果已经完成 修改当前批次任务的
		MPJLambdaWrapperX<TemuOrderBatchRelationDO> objectMPJLambdaWrapperX = new MPJLambdaWrapperX<>();
		objectMPJLambdaWrapperX
				.selectAll(TemuOrderBatchRelationDO.class)
				.leftJoin(TemuOrderDO.class, TemuOrderDO::getId, TemuOrderBatchRelationDO::getOrderId);
		switch (orderBatchTaskDO.getType()) {
			case TASK_TYPE_ART:
				//查找作图任务没有完成的记录
				objectMPJLambdaWrapperX.eq(TemuOrderDO::getIsCompleteDrawTask, ORDER_TASK_STATUS_NOT_COMPLETE);
				break;
			case TASK_TYPE_PRODUCTION:
				//查找生产任务没有完成的记录
				objectMPJLambdaWrapperX.eq(TemuOrderDO::getIsCompleteProducerTask, ORDER_TASK_STATUS_NOT_COMPLETE);
				break;
		}
		List<TemuOrderBatchRelationDO> temuOrderBatchRelationList = temuOrderBatchRelationMapper.selectJoinList(TemuOrderBatchRelationDO.class, objectMPJLambdaWrapperX);
		if (temuOrderBatchRelationList == null || temuOrderBatchRelationList.isEmpty()) {
			//更新任务完成状态
			orderBatchTaskDO.setStatus(TASK_STATUS_COMPLETE);
			orderBatchTaskMapper.updateById(orderBatchTaskDO);
			//如果批次任务类型是生产 需要修改批次任务为已完成 修改所有批次关联的订单状态得待发货
			if (orderBatchTaskDO.getType() == TASK_TYPE_PRODUCTION) {
				TemuOrderBatchUpdateStatusByTaskVO temuOrderBatchUpdateStatusByTaskVO = new TemuOrderBatchUpdateStatusByTaskVO();
				temuOrderBatchUpdateStatusByTaskVO.setId(orderBatchTaskDO.getBatchOrderId());
				temuOrderBatchUpdateStatusByTaskVO.setTaskId(orderBatchTaskDO.getId());
				updateStatusByTask(temuOrderBatchUpdateStatusByTaskVO);
			}
		}
		return 1;
	}
	
	@Override
	@Transactional
	public int completeBatchOrderTaskByAdmin(TemuOrderBatchCompleteOrderTaskByAdminVO requestVO) {
		//检查批次是否存在
		TemuOrderBatchDO temuOrderBatchDO = temuOrderBatchMapper.selectById(requestVO.getId());
		if (temuOrderBatchDO == null) {
			throw exception(ErrorCodeConstants.ORDER_BATCH_NOT_EXISTS);
		}
		//检查批次状态 已生产的批次订单不允许操作
		if (temuOrderBatchDO.getStatus() != TemuOrderBatchStatusEnum.IN_PRODUCTION) {
			throw exception(ErrorCodeConstants.ORDER_BATCH_STATUS_ERROR);
		}
		//检查订单状态
		TemuOrderDO temuOrderDO = temuOrderMapper.selectById(requestVO.getOrderId());
		if (temuOrderDO == null) {
			throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
		}
		if (temuOrderDO.getOrderStatus() != TemuOrderStatusEnum.IN_PRODUCTION) {
			throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
		}
		switch (requestVO.getTaskType()) {
			case TASK_TYPE_ART:
				temuOrderDO.setIsCompleteDrawTask(ORDER_TASK_STATUS_COMPLETE);
				break;
			case TASK_TYPE_PRODUCTION:
				//作图未完成的 不允许完成生产任务
				if (temuOrderDO.getIsCompleteDrawTask() != ORDER_TASK_STATUS_COMPLETE) {
					throw exception(ErrorCodeConstants.ORDER_BATCH_TASK_DRAW_NOT_COMPLETE);
				}
				temuOrderDO.setIsCompleteProducerTask(ORDER_TASK_STATUS_COMPLETE);
				
				break;
			default:
				throw exception(ErrorCodeConstants.ORDER_BATCH_TASK_TYPE_ERROR);
		}
		//更新订单状态
		temuOrderMapper.updateById(temuOrderDO);
		//如果是生产类型完成任务 检查 当前批次所有的订单是否已经完成
		if (requestVO.getTaskType() == TASK_TYPE_PRODUCTION) {
			MPJLambdaWrapperX<TemuOrderBatchRelationDO> objectMPJLambdaWrapperX = new MPJLambdaWrapperX<>();
			objectMPJLambdaWrapperX
					.selectAll(TemuOrderBatchRelationDO.class)
					.leftJoin(TemuOrderDO.class, TemuOrderDO::getId, TemuOrderBatchRelationDO::getOrderId);
			objectMPJLambdaWrapperX.eq(TemuOrderDO::getIsCompleteProducerTask, ORDER_TASK_STATUS_NOT_COMPLETE);
			List<TemuOrderBatchRelationDO> temuOrderBatchRelationList = temuOrderBatchRelationMapper.selectJoinList(TemuOrderBatchRelationDO.class, objectMPJLambdaWrapperX);
			//当所有任务已经完成的时候
			if (temuOrderBatchRelationList == null || temuOrderBatchRelationList.isEmpty()) {
				updateStatus(new TemuOrderBatchUpdateStatusVO().setId(requestVO.getId()));
			}
		}
		return 1;
	}
	
	
	private PageResult<TemuOrderBatchUserDetailDO> selectUserPage(TemuOrderBatchPageVO temuOrderBatchPageVO) {
		// 第一步：查询批次信息
		MPJLambdaWrapperX<TemuOrderBatchTaskDO> batchWrapper = new MPJLambdaWrapperX<>();
		batchWrapper.selectAll(TemuOrderBatchDO.class)
				.leftJoin(TemuOrderBatchDO.class, TemuOrderBatchDO::getId, TemuOrderBatchTaskDO::getBatchOrderId)
				.selectAs(TemuOrderBatchTaskDO::getId, "task_id")
				.selectAs(TemuOrderBatchTaskDO::getType, "task_type")
				.selectAs(TemuOrderBatchTaskDO::getStatus, "task_status")
				.selectAs(TemuOrderBatchTaskDO::getUserId, "task_user_id")
				.eqIfExists(TemuOrderBatchTaskDO::getStatus, temuOrderBatchPageVO.getTaskStatus())
				.eqIfExists(TemuOrderBatchTaskDO::getUserId, temuOrderBatchPageVO.getUserId())
				.eqIfExists(TemuOrderBatchDO::getStatus, temuOrderBatchPageVO.getStatus())
				.eqIfExists(TemuOrderBatchDO::getIsDispatchTask, temuOrderBatchPageVO.getIsDispatchTask())
				.like(StringUtils.isNotEmpty(temuOrderBatchPageVO.getBatchNo()), TemuOrderBatchDO::getBatchNo,
						temuOrderBatchPageVO.getBatchNo());
		
		// 时间范围查询
		if (temuOrderBatchPageVO.getCreateTime() != null && temuOrderBatchPageVO.getCreateTime().length == 2) {
			batchWrapper.between(TemuOrderBatchDO::getCreateTime, temuOrderBatchPageVO.getCreateTime()[0],
					temuOrderBatchPageVO.getCreateTime()[1]);
		}
		
		// 按照创建时间倒序排列
		batchWrapper.orderByDesc(TemuOrderBatchDO::getCreateTime);
		
		// 执行批次分页查询
		PageResult<TemuOrderBatchUserDetailDO> batchPageResult = orderBatchTaskMapper.selectJoinPage(temuOrderBatchPageVO, TemuOrderBatchUserDetailDO.class, batchWrapper);
		
		// 如果没有批次数据，直接返回空结果
		if (batchPageResult.getList().isEmpty()) {
			return new PageResult<>(new ArrayList<>(), batchPageResult.getTotal(),
					temuOrderBatchPageVO.getPageNo(), temuOrderBatchPageVO.getPageSize());
		}
		
		// 获取批次ID列表
		List<Long> batchIds = batchPageResult.getList().stream()
				.map(TemuOrderBatchDO::getId)
				.collect(Collectors.toList());
		
		// 第二步：查询批次关联的订单关系
		MPJLambdaWrapperX<TemuOrderBatchRelationDO> relationWrapper = new MPJLambdaWrapperX<>();
		relationWrapper.select(TemuOrderBatchRelationDO::getBatchId, TemuOrderBatchRelationDO::getOrderId)
				.in(TemuOrderBatchRelationDO::getBatchId, batchIds);
		List<TemuOrderBatchRelationDO> relationList = temuOrderBatchRelationMapper.selectJoinList(TemuOrderBatchRelationDO.class, relationWrapper);
		
		// 获取所有orderId
		List<Long> orderIds = relationList.stream()
				.map(TemuOrderBatchRelationDO::getOrderId)
				.collect(Collectors.toList());
		
		// 第三步：查询订单信息
		MPJLambdaWrapperX<TemuOrderDO> orderWrapper = new MPJLambdaWrapperX<>();
		orderWrapper.select(TemuOrderDO::getId, TemuOrderDO::getOrderNo, TemuOrderDO::getProductTitle, 
				TemuOrderDO::getOrderStatus, TemuOrderDO::getSku, TemuOrderDO::getSkc,
				TemuOrderDO::getSalePrice, TemuOrderDO::getCustomSku, TemuOrderDO::getQuantity,
				TemuOrderDO::getProductProperties, TemuOrderDO::getBookingTime, TemuOrderDO::getShopId,
				TemuOrderDO::getCreateTime, TemuOrderDO::getUpdateTime, TemuOrderDO::getCustomImageUrls,
				TemuOrderDO::getCustomTextList, TemuOrderDO::getProductImgUrl, TemuOrderDO::getCategoryId,
				TemuOrderDO::getCategoryName, TemuOrderDO::getEffectiveImgUrl, TemuOrderDO::getUnitPrice,
				TemuOrderDO::getTotalPrice, TemuOrderDO::getDefaultPrice, TemuOrderDO::getGoodsSn,
				TemuOrderDO::getComplianceUrl, TemuOrderDO::getRemark, TemuOrderDO::getOriginalQuantity,
				TemuOrderDO::getComplianceImageUrl, TemuOrderDO::getComplianceGoodsMergedUrl,
				TemuOrderDO::getIsCompleteDrawTask, TemuOrderDO::getIsCompleteProducerTask)
				.in(TemuOrderDO::getId, orderIds)
				.eq(StringUtils.isNotEmpty(temuOrderBatchPageVO.getCustomSku()), TemuOrderDO::getCustomSku,
						temuOrderBatchPageVO.getCustomSku())
				.orderByDesc(TemuOrderDO::getCreateTime);
		
		List<TemuOrderDO> orderList = temuOrderMapper.selectJoinList(TemuOrderDO.class, orderWrapper);
		
		// 构建orderId到订单的映射
		Map<Long, TemuOrderDO> orderMap = orderList.stream()
				.filter(order -> order != null && order.getId() != null)
				.collect(Collectors.toMap(TemuOrderDO::getId, order -> order, (existing, replacement) -> existing));
		
		// 构建batchId到orderId列表的映射
		Map<Long, List<Long>> batchOrderMap = relationList.stream()
				.filter(relation -> relation != null && relation.getBatchId() != null && relation.getOrderId() != null)
				.collect(Collectors.groupingBy(
						TemuOrderBatchRelationDO::getBatchId,
						Collectors.mapping(TemuOrderBatchRelationDO::getOrderId, Collectors.toList())
				));
		
		// 组装最终结果
		List<TemuOrderBatchUserDetailDO> resultList = new ArrayList<>();
		for (TemuOrderBatchUserDetailDO batch : batchPageResult.getList()) {
			List<Long> batchOrderIds = batchOrderMap.get(batch.getId());
			if (batchOrderIds != null && !batchOrderIds.isEmpty()) {
				List<TemuOrderDetailDO> orders = batchOrderIds.stream()
						.map(orderMap::get)
						.filter(Objects::nonNull)
						.map(order -> {
							TemuOrderDetailDO orderDetail = new TemuOrderDetailDO();
							BeanUtils.copyProperties(order, orderDetail);
							return orderDetail;
						})
						.collect(Collectors.toList());
				batch.setOrderList(orders);
			} else {
				batch.setOrderList(new ArrayList<>());
			}
			resultList.add(batch);
		}
		
		return new PageResult<>(resultList, batchPageResult.getTotal(),
				temuOrderBatchPageVO.getPageNo(), temuOrderBatchPageVO.getPageSize());
	}
	
}
