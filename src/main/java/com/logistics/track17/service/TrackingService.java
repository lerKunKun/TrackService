package com.logistics.track17.service;

import com.logistics.track17.dto.*;
import com.logistics.track17.entity.Carrier;
import com.logistics.track17.entity.Parcel;
import com.logistics.track17.entity.TrackingEvent;
import com.logistics.track17.entity.TrackingNumber;
import com.logistics.track17.exception.BusinessException;
import com.logistics.track17.mapper.ParcelMapper;
import com.logistics.track17.mapper.TrackingEventMapper;
import com.logistics.track17.mapper.TrackingNumberMapper;
import com.logistics.track17.util.Track17V2Parser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 运单服务
 */
@Slf4j
@Service
public class TrackingService {

    private final TrackingNumberMapper trackingNumberMapper;
    private final TrackingEventMapper trackingEventMapper;
    private final ParcelMapper parcelMapper;
    private final Track17Service track17Service;
    private final CarrierService carrierService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 运单详情缓存配置
    private static final String TRACKING_CACHE_PREFIX = "tracking:detail:";
    private static final long TRACKING_CACHE_EXPIRE_SECONDS = 300; // 5分钟过期

    public TrackingService(TrackingNumberMapper trackingNumberMapper,
            TrackingEventMapper trackingEventMapper,
            ParcelMapper parcelMapper,
            Track17Service track17Service,
            CarrierService carrierService) {
        this.trackingNumberMapper = trackingNumberMapper;
        this.trackingEventMapper = trackingEventMapper;
        this.parcelMapper = parcelMapper;
        this.track17Service = track17Service;
        this.carrierService = carrierService;
    }

    /**
     * 手动添加运单
     */
    @Transactional(rollbackFor = Exception.class)
    public TrackingResponse create(TrackingRequest request) {
        log.info("Creating tracking number: {} (carrier: {})",
                request.getTrackingNumber(),
                StringUtils.isBlank(request.getCarrierCode()) ? "auto" : request.getCarrierCode());

        // 先注册到17Track并查询，获取真实的carrier信息
        String actualCarrierCode = request.getCarrierCode();
        String actualCarrierName = null;
        Integer carrierId = null;

        try {
            // 注册到17Track
            Track17RegisterResponse registerResponse = track17Service.registerTracking(
                    request.getTrackingNumber(),
                    request.getCarrierCode());

            // 检查运单号是否被拒绝（无效）
            if (registerResponse != null && registerResponse.getData() != null
                    && registerResponse.getData().getRejected() != null
                    && !registerResponse.getData().getRejected().isEmpty()) {

                Track17RegisterResponse.Track17RegisterData.RejectedItem rejectedItem = registerResponse.getData()
                        .getRejected().get(0);
                String errorMsg = rejectedItem.getError() != null
                        ? rejectedItem.getError().getMessage()
                        : "运单号无效";

                log.warn("Tracking number rejected by 17Track: {} - {}",
                        request.getTrackingNumber(), errorMsg);
                throw BusinessException.of("运单号无效或不被支持: " + errorMsg);
            }

            // 从注册响应中获取carrier ID
            if (registerResponse != null && registerResponse.getData() != null
                    && registerResponse.getData().getAccepted() != null
                    && !registerResponse.getData().getAccepted().isEmpty()) {

                Track17RegisterResponse.Track17RegisterData.AcceptedItem acceptedItem = registerResponse.getData()
                        .getAccepted().get(0);
                carrierId = acceptedItem.getCarrier();

                if (StringUtils.isBlank(actualCarrierCode) && carrierId != null) {
                    actualCarrierCode = convertCarrierIdToCode(carrierId);
                    log.info("17Track auto-detected carrier: {} (ID: {})", actualCarrierCode, carrierId);
                }
            }

            // 尝试查询获取更多信息（如carrier名称）
            try {
                Track17QueryResponse queryResponse = track17Service.queryTracking(
                        request.getTrackingNumber(),
                        request.getCarrierCode());

                if (queryResponse != null && queryResponse.getData() != null
                        && queryResponse.getData().getAccepted() != null
                        && !queryResponse.getData().getAccepted().isEmpty()) {

                    Track17QueryResponse.Track17Data.AcceptedItem item = queryResponse.getData().getAccepted().get(0);
                    if (item.getTrack() != null) {
                        // 从17Track返回的数据中获取carrier名称
                        actualCarrierName = item.getTrack().getW1();
                        log.info("17Track carrier name: {}", actualCarrierName);
                    }
                }
            } catch (Exception e) {
                log.debug("Query tracking info not available yet (expected for new tracking): {}", e.getMessage());
                // 查询失败没关系，可能是运单号太新还没有数据
            }
        } catch (Exception e) {
            log.error("Failed to register tracking to 17Track: {}", request.getTrackingNumber(), e);
            throw BusinessException.of("运单号注册失败，请检查运单号是否正确或稍后重试");
        }

        // 创建或获取包裹
        Parcel parcel;
        if (request.getOrderId() != null) {
            parcel = parcelMapper.selectByOrderId(request.getOrderId());
            if (parcel == null) {
                parcel = new Parcel();
                parcel.setOrderId(request.getOrderId());
                parcel.setCarrierCode(actualCarrierCode);
                parcel.setCarrierName(actualCarrierName);
                parcel.setStatus("in_transit");
                parcelMapper.insert(parcel);
            }
        } else {
            parcel = new Parcel();
            parcel.setCarrierCode(actualCarrierCode);
            parcel.setCarrierName(actualCarrierName);
            parcel.setStatus("in_transit");
            parcelMapper.insert(parcel);
        }

        // 创建运单
        TrackingNumber trackingNumber = new TrackingNumber();
        trackingNumber.setParcelId(parcel.getId());
        trackingNumber.setTrackingNumber(request.getTrackingNumber());
        trackingNumber.setCarrierCode(actualCarrierCode);
        trackingNumber.setSource(StringUtils.isNotBlank(request.getSource()) ? request.getSource() : "manual");
        trackingNumber.setTrackStatus("InfoReceived");

        // 插入数据库（利用唯一约束保证并发安全）
        try {
            trackingNumberMapper.insert(trackingNumber);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 违反唯一约束，说明运单号已存在
            log.warn("Duplicate tracking number detected: {}", request.getTrackingNumber());
            throw BusinessException.of("运单号已存在: " + request.getTrackingNumber());
        }

        log.info("Tracking number created successfully: {}", trackingNumber.getId());
        return convertToResponse(trackingNumber);
    }

    /**
     * 转换17Track carrier ID为系统carrier代码
     * 使用数据库映射表
     */
    private String convertCarrierIdToCode(Integer carrierId) {
        if (carrierId == null) {
            return "unknown";
        }

        // 从数据库查询映射
        Carrier carrier = carrierService.getByCarrierId(carrierId);
        if (carrier != null) {
            return carrier.getCarrierCode();
        }

        // 如果数据库中没有，返回carrier-{ID}格式
        log.info("Unknown carrier ID: {}, using carrier-{}", carrierId, carrierId);
        return "carrier-" + carrierId;
    }

    /**
     * 获取运单列表（分页、筛选、搜索）
     */
    public PageResult<TrackingResponse> getList(String keyword, Long shopId, String status,
            String carrierCode, String startDate, String endDate,
            Integer page, Integer pageSize) {
        page = page == null || page < 1 ? 1 : page;
        pageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);

        int offset = (page - 1) * pageSize;

        List<TrackingNumber> trackingNumbers = trackingNumberMapper.selectList(
                keyword, shopId, status, carrierCode, startDate, endDate, offset, pageSize);

        Long total = trackingNumberMapper.count(keyword, shopId, status, carrierCode, startDate, endDate);

        List<TrackingResponse> responses = trackingNumbers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(responses, total, page, pageSize);
    }

    /**
     * 获取已使用的承运商列表（用于筛选）
     */
    public List<String> getUsedCarriers() {
        return trackingNumberMapper.selectDistinctCarriers();
    }

    /**
     * 获取运单详情（带缓存）
     */
    public TrackingResponse getById(Long id) {
        // 先查缓存
        String cacheKey = TRACKING_CACHE_PREFIX + id;
        TrackingResponse cached = (TrackingResponse) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.debug("Cache hit for tracking: {}", id);
            return cached;
        }

        // 缓存未命中，查数据库
        log.debug("Cache miss for tracking: {}", id);
        TrackingNumber trackingNumber = trackingNumberMapper.selectById(id);
        if (trackingNumber == null) {
            throw BusinessException.of(404, "运单不存在");
        }

        TrackingResponse response = convertToResponse(trackingNumber);

        // 获取物流事件
        List<TrackingEvent> events = trackingEventMapper.selectByTrackingId(id);
        List<TrackingEventResponse> eventResponses = events.stream()
                .map(this::convertToEventResponse)
                .collect(Collectors.toList());
        response.setEvents(eventResponses);

        // 写入缓存（5分钟过期）
        redisTemplate.opsForValue().set(cacheKey, response,
                TRACKING_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.debug("Cached tracking: {}", id);

        return response;
    }

    /**
     * 手动同步运单状态（使用V2 API）
     */
    @Transactional(rollbackFor = Exception.class)
    public TrackingResponse sync(Long id) {
        log.info("Manually syncing tracking number with V2 API: {}", id);

        TrackingNumber trackingNumber = trackingNumberMapper.selectById(id);
        if (trackingNumber == null) {
            throw BusinessException.of(404, "运单不存在");
        }

        try {
            // 调用17Track V2 API查询
            Track17V2Response queryResponse = track17Service.queryTrackingV2(
                    trackingNumber.getTrackingNumber(),
                    trackingNumber.getCarrierCode());

            // 解析17Track V2返回的数据
            if (queryResponse != null && queryResponse.getData() != null) {
                Track17V2Response.Track17V2Data data = queryResponse.getData();

                // 检查是否有成功接收的数据
                if (data.getAccepted() != null && !data.getAccepted().isEmpty()) {
                    Track17V2Response.AcceptedItem item = data.getAccepted().get(0);

                    // 使用V2解析器更新运单主信息
                    Track17V2Parser.parseAndUpdateTracking(item, trackingNumber);

                    // 同步时更新carrier_code为正确的映射（如果carrier_id有值）
                    if (item.getCarrier() != null) {
                        String carrierCode = convertCarrierIdToCode(item.getCarrier());
                        trackingNumber.setCarrierCode(carrierCode);
                        log.info("Updated carrier_code to: {} (from carrier_id: {})", carrierCode, item.getCarrier());
                    }

                    trackingNumberMapper.update(trackingNumber);

                    // 清除旧事件
                    trackingEventMapper.deleteByTrackingId(id);

                    // 使用批量插入保存新事件（性能优化）
                    List<TrackingEvent> events = Track17V2Parser.parseEvents(item, id);
                    if (!events.isEmpty()) {
                        trackingEventMapper.insertBatch(events);
                        log.info("Batch saved {} tracking events", events.size());
                    }

                    log.info("Tracking number synced successfully with V2 data: {}", id);
                } else if (data.getRejected() != null && !data.getRejected().isEmpty()) {
                    // 处理被拒绝的运单
                    Track17V2Response.RejectedItem rejected = data.getRejected().get(0);
                    log.warn("Tracking query rejected: {} - {}",
                            rejected.getError().getCode(),
                            rejected.getError().getMessage());
                    throw BusinessException.of("查询失败: " + rejected.getError().getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to sync tracking number: {}", id, e);
            throw BusinessException.of("同步失败: " + e.getMessage());
        }

        // 清除缓存
        String cacheKey = TRACKING_CACHE_PREFIX + id;
        redisTemplate.delete(cacheKey);
        log.debug("Cache invalidated for tracking: {}", id);

        return getById(id);
    }

    /**
     * 转换17Track状态码为系统状态
     */
    private String convertTrackStatus(Integer z0) {
        if (z0 == null) {
            return "InfoReceived";
        }
        switch (z0) {
            case 0:
            case 1:
                return "InfoReceived"; // 待查询、查询中
            case 2:
                return "InTransit"; // 运输途中
            case 3:
            case 4:
                return "Delivered"; // 到达待取、成功签收
            case 5:
            case 6:
                return "Exception"; // 疑难件、退件签收
            default:
                return "InfoReceived";
        }
    }

    /**
     * 解析事件时间
     */
    private LocalDateTime parseEventTime(String timeStr) {
        if (timeStr == null || timeStr.length() < 14) {
            return LocalDateTime.now();
        }
        try {
            // 格式: 20240101120000
            int year = Integer.parseInt(timeStr.substring(0, 4));
            int month = Integer.parseInt(timeStr.substring(4, 6));
            int day = Integer.parseInt(timeStr.substring(6, 8));
            int hour = Integer.parseInt(timeStr.substring(8, 10));
            int minute = Integer.parseInt(timeStr.substring(10, 12));
            int second = Integer.parseInt(timeStr.substring(12, 14));

            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (Exception e) {
            log.warn("Failed to parse event time: {}", timeStr);
            return LocalDateTime.now();
        }
    }

    /**
     * 删除运单
     */
    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新备注
     */
    public TrackingResponse updateRemarks(Long id, String remarks) {
        log.info("Updating remarks for tracking number: {}, remarks: {}", id, remarks);

        TrackingNumber trackingNumber = trackingNumberMapper.selectById(id);
        if (trackingNumber == null) {
            throw BusinessException.of(404, "运单不存在");
        }

        // P1优化：使用乐观锁避免并发更新丢失
        Integer oldVersion = trackingNumber.getVersion();
        trackingNumber.setRemarks(remarks);
        int updated = trackingNumberMapper.update(trackingNumber);

        if (updated == 0) {
            // 更新失败，说明版本冲突
            log.warn("Version conflict when updating remarks for tracking: {}, version: {}", id, oldVersion);
            throw BusinessException.of("数据已被其他用户修改，请刷新后重试");
        }

        // 清除缓存
        String cacheKey = TRACKING_CACHE_PREFIX + id;
        redisTemplate.delete(cacheKey);
        log.debug("Cache invalidated for tracking: {}", id);

        log.info("Remarks updated successfully for tracking number: {}", id);
        return getById(id);
    }

    /**
     * 删除运单（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        log.info("Soft deleting tracking number: {}", id);

        TrackingNumber trackingNumber = trackingNumberMapper.selectById(id);
        if (trackingNumber == null) {
            throw BusinessException.of(404, "运单不存在");
        }

        // 软删除：设置 deleted_at 时间戳
        trackingNumberMapper.deleteById(id);

        // 清除缓存
        String cacheKey = TRACKING_CACHE_PREFIX + id;
        redisTemplate.delete(cacheKey);
        log.debug("Cache invalidated for tracking: {}", id);

        log.info("Tracking number soft deleted successfully: {}", id);
    }

    /**
     * 批量删除运单（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        log.info("Batch soft deleting tracking numbers: {}", ids.size());

        // 批量软删除运单
        if (!ids.isEmpty()) {
            trackingNumberMapper.deleteBatch(ids);

            // 清除缓存
            for (Long id : ids) {
                String cacheKey = TRACKING_CACHE_PREFIX + id;
                redisTemplate.delete(cacheKey);
            }
        }

        log.info("Batch soft delete completed: {}", ids.size());
    }

    /**
     * 批量导入运单
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResult batchImport(BatchImportRequest request) {
        log.info("Batch importing tracking numbers: {}", request.getItems().size());

        int total = request.getItems().size();
        int success = 0;
        int failed = 0;

        // P1优化：批量检查重复
        List<String> trackingNumbersToCheck = request.getItems().stream()
                .map(BatchImportItem::getTrackingNumber)
                .collect(Collectors.toList());

        List<TrackingNumber> existingTrackings = trackingNumberMapper.selectByTrackingNumbers(trackingNumbersToCheck);
        java.util.Set<String> existingNumbers = existingTrackings.stream()
                .map(TrackingNumber::getTrackingNumber)
                .collect(Collectors.toSet());

        // P1优化：批量处理
        List<TrackingNumber> trackingNumbersToInsert = new java.util.ArrayList<>();
        List<Parcel> parcelsToInsert = new java.util.ArrayList<>();

        for (BatchImportItem item : request.getItems()) {
            try {
                // 跳过已存在的运单
                if (existingNumbers.contains(item.getTrackingNumber())) {
                    log.warn("Tracking number already exists, skipping: {}", item.getTrackingNumber());
                    failed++;
                    continue;
                }

                String actualCarrierCode = item.getCarrierCode();
                String actualCarrierName = null;
                Integer carrierId = null;

                // 注意：17Track API调用保持串行，因为需要实时结果
                // 可选优化：使用CompletableFuture并行调用，但需要处理限流
                try {
                    Track17RegisterResponse registerResponse = track17Service.registerTracking(
                            item.getTrackingNumber(),
                            item.getCarrierCode());

                    // 检查运单号是否被拒绝（无效）
                    if (registerResponse != null && registerResponse.getData() != null
                            && registerResponse.getData().getRejected() != null
                            && !registerResponse.getData().getRejected().isEmpty()) {

                        Track17RegisterResponse.Track17RegisterData.RejectedItem rejectedItem = registerResponse
                                .getData().getRejected().get(0);
                        String errorMsg = rejectedItem.getError() != null
                                ? rejectedItem.getError().getMessage()
                                : "运单号无效";

                        log.warn("Tracking number rejected by 17Track: {} - {}", item.getTrackingNumber(), errorMsg);
                        failed++;
                        continue; // 跳过无效运单号
                    }

                    if (registerResponse != null && registerResponse.getData() != null
                            && registerResponse.getData().getAccepted() != null
                            && !registerResponse.getData().getAccepted().isEmpty()) {

                        Track17RegisterResponse.Track17RegisterData.AcceptedItem acceptedItem = registerResponse
                                .getData().getAccepted().get(0);
                        carrierId = acceptedItem.getCarrier();

                        if (StringUtils.isBlank(actualCarrierCode) && carrierId != null) {
                            actualCarrierCode = convertCarrierIdToCode(carrierId);
                            log.debug("17Track auto-detected carrier for {}: {} (ID: {})",
                                    item.getTrackingNumber(), actualCarrierCode, carrierId);
                        }
                    }

                    try {
                        Track17QueryResponse queryResponse = track17Service.queryTracking(
                                item.getTrackingNumber(),
                                item.getCarrierCode());

                        if (queryResponse != null && queryResponse.getData() != null
                                && queryResponse.getData().getAccepted() != null
                                && !queryResponse.getData().getAccepted().isEmpty()) {

                            Track17QueryResponse.Track17Data.AcceptedItem queryItem = queryResponse.getData()
                                    .getAccepted().get(0);
                            if (queryItem.getTrack() != null) {
                                actualCarrierName = queryItem.getTrack().getW1();
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Query tracking info not available yet for {}: {}",
                                item.getTrackingNumber(), e.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Failed to register tracking to 17Track for {}: {}", item.getTrackingNumber(),
                            e.getMessage());
                    failed++;
                    continue; // 跳过注册失败的运单号
                }

                // 创建包裹对象
                Parcel parcel = new Parcel();
                parcel.setCarrierCode(actualCarrierCode);
                parcel.setCarrierName(actualCarrierName);
                parcel.setStatus("in_transit");
                parcelMapper.insert(parcel); // 包裹仍需逐个插入以获取ID

                // 创建运单对象
                TrackingNumber trackingNumber = new TrackingNumber();
                trackingNumber.setParcelId(parcel.getId());
                trackingNumber.setTrackingNumber(item.getTrackingNumber());
                trackingNumber.setCarrierCode(actualCarrierCode);
                trackingNumber.setSource("batch_import");
                trackingNumber.setTrackStatus("InfoReceived");

                trackingNumbersToInsert.add(trackingNumber);
                success++;
            } catch (Exception e) {
                log.error("Failed to prepare tracking number: {}", item.getTrackingNumber(), e);
                failed++;
            }
        }

        // P1优化：批量插入运单（每500条一批）
        if (!trackingNumbersToInsert.isEmpty()) {
            int batchSize = 500;
            for (int i = 0; i < trackingNumbersToInsert.size(); i += batchSize) {
                int end = Math.min(i + batchSize, trackingNumbersToInsert.size());
                List<TrackingNumber> batch = trackingNumbersToInsert.subList(i, end);
                try {
                    trackingNumberMapper.insertBatch(batch);
                    log.debug("Batch inserted {} tracking numbers ({}-{})", batch.size(), i, end);
                } catch (Exception e) {
                    log.error("Failed to batch insert tracking numbers: {}-{}", i, end, e);
                    // 批量失败时回退到逐条插入
                    for (TrackingNumber tn : batch) {
                        try {
                            trackingNumberMapper.insert(tn);
                        } catch (Exception ex) {
                            log.error("Failed to insert tracking number: {}", tn.getTrackingNumber(), ex);
                            success--;
                            failed++;
                        }
                    }
                }
            }
        }

        String message = String.format("导入完成: 成功 %d, 失败 %d", success, failed);
        log.info("Batch import completed: total={}, success={}, failed={}", total, success, failed);

        return new BatchImportResult(total, success, failed, message);
    }

    /**
     * 转换为响应DTO
     */
    private TrackingResponse convertToResponse(TrackingNumber trackingNumber) {
        TrackingResponse response = new TrackingResponse();
        BeanUtils.copyProperties(trackingNumber, response);
        return response;
    }

    /**
     * 转换事件为响应DTO
     */
    private TrackingEventResponse convertToEventResponse(TrackingEvent event) {
        TrackingEventResponse response = new TrackingEventResponse();
        BeanUtils.copyProperties(event, response);
        // 手动映射字段名不匹配的字段
        response.setDescription(event.getEventDescription()); // eventDescription -> description
        response.setLocation(event.getEventLocation()); // eventLocation -> location
        return response;
    }
}
