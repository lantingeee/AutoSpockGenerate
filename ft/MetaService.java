package com.ctrip.hotel.order.pkg.meta.service;

import com.ctrip.hotel.order.common.configuration.ConfigProvider;
import com.ctrip.hotel.order.common.log.EsContext;
import com.ctrip.hotel.order.common.soa.SoaServiceHelper;
import com.ctrip.hotel.order.common.util.EsHelper;
import com.ctrip.hotel.order.pkg.meta.errorcode.MetaErrorCode;
import com.ctrip.hotel.order.pkg.meta.exception.BizException;
import com.ctrip.hotel.order.pkg.meta.meta.DataServiceConfiguration;
import com.ctrip.hotel.order.pkg.meta.meta.dto.GenMetaXOrderDTO;
import com.ctrip.hotel.order.pkg.meta.constants.Domain;
import com.ctrip.hotel.order.pkg.meta.meta.model.CusMetaType;
import com.ctrip.hotel.order.processservice.contract.*;
import com.ctrip.hotel.order.service.component.util.CollectionUtils;
import com.ctrip.hotel.order.service.component.util.JacksonUtils;
import com.ctrip.hotel.order.xproduct.*;
import com.ctrip.hotel.order.xproduct.pkg.meta.OrderConfirmMeta;
import com.ctrip.presale.orderdb.model.OSourceCusRelation;
import com.ctrip.presale.orderdb.model.OXCusMeta;
import com.ctrip.presale.orderdb.model.OXOrder;
import com.ctrip.presale.orderdb.service.DataService;
import com.google.common.collect.Lists;
import com.googlecode.aviator.Expression;
import hotel.order.supplier.common.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MetaService {
    @Resource(name = "pkgConfirmMetaExpression")
    private Expression pkgConfirmMetaExpression;

    @Resource(name = "orderConfirmMetaExpression")
    private Expression orderConfirmMetaExpression;

    @Resource(name = "orderConfirmResultMetaExpression")
    private Expression orderConfirmResultMetaExpression;

    public GenerateMetaResponseType generateMeta(GenerateMetaRequestType request) {
        EsContext esContext = SoaServiceHelper.buildEsContext(request.getSourceOrderId(), request);
        GenerateMetaResponseType response = new GenerateMetaResponseType();
        if (ConfigProvider.getInstance().disable("OpenMeta")) {
            return response;
        }
        try {
            Map<String, Object> pkgOrderMeta = new HashMap<>();
            pkgOrderMeta.put("hotelOrderId", request.getSourceOrderId());
            pkgConfirmMetaExpression.execute(pkgOrderMeta);

            Map<String, Object> confirmMap = new HashMap<>();
            List<GenMetaXOrderDTO> dtoList = this.queryXGenOrderDTO(request.getSourceOrderId());
            confirmMap.put("xOrders", dtoList);

            orderConfirmMetaExpression.execute(confirmMap);
            orderConfirmResultMetaExpression.execute(confirmMap);

            response.setPkgMeta(this.buildPkgMetaByMap(pkgOrderMeta));
            response.setOrderMeta(this.buildOrderMetaByMap(confirmMap));

            // 插入 o_xCusMeta 表
            this.insertCusOrder(response);
            ResultType resultType = new ResultType();
            resultType.setIsSuccessful(true);
            response.setResult(resultType);
            return response;
        } catch (Exception e) {
            log.error("generateMeta", e);
            return buildGenFailResponse(e);
        } finally {
            esContext.responseContent(JacksonUtils.toJsonString(response));
            EsHelper.addEsLog(esContext);
        }
    }

    public GenerateMetaResponseType buildGenFailResponse(Exception e) {
        GenerateMetaResponseType responseType = new GenerateMetaResponseType();
        ResultType resultType = new ResultType();
        resultType.setIsSuccessful(false);
        resultType.setCode(MetaErrorCode.SYSTEM_ERROR.getKey());
        resultType.setMsg(MetaErrorCode.SYSTEM_ERROR.getValue());
        responseType.setResult(resultType);
        return responseType;
    }

    private List<GenMetaXOrderDTO> queryXGenOrderDTO(Long hotelOrderId) {
        List<GenMetaXOrderDTO> dtoList = Lists.newArrayList();

        ReadOrderRequestType requestType = new ReadOrderRequestType();
        requestType.setOrderID(Lists.newArrayList(String.valueOf(hotelOrderId.longValue())));
        RequestHeadType head = new RequestHeadType();
        head.setTimeStamp(DateUtil.formatLongerDate(new Date()));
        head.setRequestType("Hotel.Order.ProcessService.ReadOrder");
        head.setRequestor("Online");
        head.setRequestID(UUID.randomUUID().toString());
        head.setClientAppID("100029870");
        head.setServerFrom("Online");
        requestType.setRequestHead(head);
        ReadOrderResponseType response = SoaServiceHelper.invoke(hotelOrderId,
                HotelOrderProcessServiceClient.getInstance()::readOrder, requestType);
        if (response == null || CollectionUtils.isEmpty(response.getOrderList())) {
            log.warn("invoke readOrder empty: req:{}, resp:{}", JacksonUtils.toJsonString(requestType), JacksonUtils.toJsonString(response));
            return dtoList;
        }
        List<OrderInfoType> orderList = response.getOrderList();
        BigDecimal hotelCancelPolicy = this.getHotelCancelPolicy(orderList);
        if (CollectionUtils.isEmpty(orderList)) {
            return dtoList;
        }
        List<GenMetaXOrderDTO> metaXOrderDTOList = this.getXCancelAmount(orderList);
        BigDecimal allXCancelAmount = metaXOrderDTOList.stream().map(GenMetaXOrderDTO::getXCancelAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("allXCancelAmount: {}, hotelCancelPolicy: {}", allXCancelAmount, hotelCancelPolicy);
        metaXOrderDTOList.forEach(i -> i.setAllXCancelAmount(allXCancelAmount));
        metaXOrderDTOList.forEach(i -> i.setHotelCancelAmount(hotelCancelPolicy));
        return metaXOrderDTOList;
    }

    private BigDecimal getHotelCancelPolicy(List<OrderInfoType> hotelOrders) {
        if (CollectionUtils.isEmpty(hotelOrders)) {
            return new BigDecimal(0);
        }
        OrderInfoType orderInfoType = hotelOrders.get(0);
        if (orderInfoType == null || orderInfoType.getCancel() == null || orderInfoType.getCancel().getCancelPolicy() == null) {
            return new BigDecimal(0);
        }
        CancelPolicyInfo cancelPolicy = orderInfoType.getCancel().getCancelPolicy();
        return CancelLadderInfoService.calCancelPolicyAmount(cancelPolicy);
    }

    private List<GenMetaXOrderDTO> getXCancelAmount(List<OrderInfoType> orderList) {
        ArrayList<GenMetaXOrderDTO> genMetaXOrderDTOS = Lists.newArrayList();
        if (CollectionUtils.isEmpty(orderList)) {
            return genMetaXOrderDTOS;
        }
        OrderInfoType orderInfoType = orderList.get(0);
        Integer countryID = orderInfoType.getSummaryInfo().getHotel().getCity().getCountryID();
        OrderDetailInfoType detailInfo = orderInfoType.getDetailInfo();
        if (detailInfo == null || detailInfo.getReservationInfo() == null || detailInfo.getReservationInfo().getOrderPkgProductInfo() == null ||
                CollectionUtils.isEmpty(detailInfo.getReservationInfo().getOrderPkgProductInfo().getXProductInfo())) {
            return Lists.newArrayList();
        }
        List<OrdXProductInfoType> xProductInfoList = detailInfo.getReservationInfo().getOrderPkgProductInfo().getXProductInfo();

        List<Long> xOrderIds = Lists.newArrayList();
        for (OrdXProductInfoType xProductInfo : xProductInfoList) {
            if (xOrderIds.contains(xProductInfo.getXOrderId())) {
                continue;
            }
            xOrderIds.add(xProductInfo.getXOrderId());
            GenMetaXOrderDTO metaXOrderDTO = new GenMetaXOrderDTO();
            genMetaXOrderDTOS.add(metaXOrderDTO);
            metaXOrderDTO.setXOrderId(xProductInfo.getXOrderId());
            CancelPolicyInfo cancelPolicyInfo = new CancelPolicyInfo();
            cancelPolicyInfo.setOriginalLadderPolicy(xProductInfo.getCancelLadder());
            metaXOrderDTO.setXCancelAmount(CancelLadderInfoService.calCancelPolicyAmount(cancelPolicyInfo));
            metaXOrderDTO.setSpuId(Optional.ofNullable(xProductInfo.getSpuId()).orElse(0L));
            metaXOrderDTO.setCountry(countryID);
        }
        return genMetaXOrderDTOS;
    }

    private PkgOrderMetaType buildPkgMetaByMap(Map<String, Object> pkgOrderMeta) {
        PkgOrderMetaType metaType = new PkgOrderMetaType();
        PkgConfirmMetaType confirmMetaType = new PkgConfirmMetaType();
        confirmMetaType.setRelation((String) pkgOrderMeta.get("pkgRelation"));
        Map<Long, Long> lastWaitingStr = (Map<Long, Long>) pkgOrderMeta.get("lastWaitingTimeMap");
        Map<String, Long> lastWaitingTime = new HashMap<>();
        lastWaitingStr.forEach((k, v) -> lastWaitingTime.put(String.valueOf(k), v));
        confirmMetaType.setLastWaitingTime(lastWaitingTime);
        metaType.setPkgConfirmMeta(confirmMetaType);
        return metaType;
    }

    private List<OrderMetaType> buildOrderMetaByMap(Map<String, Object> confirmMap) {
        List<GenMetaXOrderDTO> orders = (List<GenMetaXOrderDTO>) confirmMap.get("xOrders");
        List<OrderMetaType> list = Lists.newArrayList();
        if (CollectionUtils.isEmpty(orders)) {
            return list;
        }
        for (GenMetaXOrderDTO order : orders) {
            OrderMetaType metaType = new OrderMetaType();
            metaType.setOrderId(order.getXOrderId());
            OrderConfirmMeta orderConfirmMeta = new OrderConfirmMeta();
            orderConfirmMeta.setMode(order.getMode());
            orderConfirmMeta.setPriority(order.getPriority());
            orderConfirmMeta.setLastConfirmTime(order.getLastConfirmTime());
            metaType.setOrderConfirmMeta(orderConfirmMeta);
            List<OrderConfirmResultMeta> orderConfirmResultMetaList = Lists.newArrayList();

            Map<String, String> confirmResultList = order.getConfirmResultList();
            confirmResultList.forEach((k, v) -> {
                OrderConfirmResultMeta confirmResultMeta = new OrderConfirmResultMeta();
                confirmResultMeta.setResult(k);
                confirmResultMeta.setAction(v);
                orderConfirmResultMetaList.add(confirmResultMeta);
            });
            metaType.setOrderConfirmResultMetaList(orderConfirmResultMetaList);
            list.add(metaType);
        }
        return list;
    }

    private void insertCusOrder( GenerateMetaResponseType response) {
        List<OXCusMeta> cusMetaList = Lists.newArrayList();

        List<OrderMetaType> orderMetaList = response.getOrderMeta();
        for (OrderMetaType metaType : orderMetaList) {
            OXCusMeta cusMeta = new OXCusMeta();
            cusMeta.setXOrderId(metaType.getOrderId());
            if (metaType.getOrderId() == null) {
                continue;
            }
            CusMetaType cusMetaType = new CusMetaType();
            cusMetaType.setPkgMeta(response.getPkgMeta());
            cusMetaType.setOrderMeta(metaType);
            cusMeta.setCusOrderMeta(JacksonUtils.toJsonString(cusMetaType));
            cusMetaList.add(cusMeta);
        }
        DataService dataService = DataServiceConfiguration.getBean(DataService.class);
        dataService.batchInsert(cusMetaList);
    }

    public QueryOrderMetaInfoResponseType queryOrderMetaInfo(QueryOrderMetaInfoRequestType requestType) {
        EsContext esContext = SoaServiceHelper.buildEsContext(requestType.getSourceOrderId(), requestType);
        QueryOrderMetaInfoResponseType responseType = new QueryOrderMetaInfoResponseType();
        try {
            if (requestType.getSourceOrderId() == null && CollectionUtils.isEmpty(requestType.getXOrderIds())) {
                log.warn("request source or xOrderIds empty:" + JacksonUtils.toJsonString(requestType));
                throw new BizException(MetaErrorCode.PARAM_INFO_ERROR.getKey(), MetaErrorCode.PARAM_INFO_ERROR.getValue());
            }
            DataService dataService = DataServiceConfiguration.getBean(DataService.class);
            if (requestType.getSourceOrderId() == null) {
                OXOrder oxOrder = new OXOrder();
                oxOrder.setOrderId(requestType.getXOrderIds().get(0));
                List<OXOrder> oxOrders = dataService.selectList(oxOrder);
                requestType.setSourceOrderId(oxOrders.get(0).getSourceOrderId());
                esContext.orderId(oxOrders.get(0).getSourceOrderId());
            }

            OSourceCusRelation condition = new OSourceCusRelation();
            condition.setSourceOrderId(requestType.getSourceOrderId());
            List<OSourceCusRelation> oSourceCusRelations = dataService.selectList(condition);

            List<OXCusMeta> cusMetas = Lists.newArrayList();
            for (OSourceCusRelation oSourceCusRelation : oSourceCusRelations) {
                OXCusMeta cusMeta = new OXCusMeta();
                cusMeta.setXOrderId(oSourceCusRelation.getCusOrderId());
                cusMetas.addAll(dataService.selectList(cusMeta));
            }
            responseType = this.buildMetaResponse(cusMetas);
            List<MetaProcessStrategy> processStrategyList = this.calRealTimeStrategy(requestType.getSourceOrderId());
            responseType.setProcessStrategyList(processStrategyList);
            return responseType;
        } catch (BizException ex) {
            log.warn("queryOrderMetaInfo, bizExc: {}, {}", ex.getCode(), ex.getMessage());
            return buildFailResponse(ex);
        } catch (Exception e) {
            log.error("queryOrderMetaInfo", e);
            return buildFailResponse();
        } finally {
            esContext.responseContent(JacksonUtils.toJsonString(responseType));
            EsHelper.addEsLog(esContext);
        }
    }

    public QueryOrderMetaInfoResponseType buildFailResponse(BizException ex) {
        QueryOrderMetaInfoResponseType responseType = new QueryOrderMetaInfoResponseType();
        ResultType resultType = new ResultType();
        resultType.setIsSuccessful(false);
        resultType.setCode(ex.getCode());
        resultType.setMsg(ex.getMessage());
        responseType.setResult(resultType);
        return responseType;
    }

    public QueryOrderMetaInfoResponseType buildFailResponse() {
        QueryOrderMetaInfoResponseType responseType = new QueryOrderMetaInfoResponseType();
        ResultType resultType = new ResultType();
        resultType.setIsSuccessful(false);
        resultType.setCode(MetaErrorCode.SYSTEM_ERROR.getKey());
        resultType.setMsg(MetaErrorCode.SYSTEM_ERROR.getValue());
        responseType.setResult(resultType);
        return responseType;
    }

    private QueryOrderMetaInfoResponseType buildMetaResponse(List<OXCusMeta> cusMetas) {
        QueryOrderMetaInfoResponseType responseType = new QueryOrderMetaInfoResponseType();
        if (CollectionUtils.isEmpty(cusMetas)) {
            return responseType;
        }

        PkgOrderMetaType pkgMeta = new PkgOrderMetaType();
        List<OrderMetaType> orderMetaList = Lists.newArrayList();
        for (OXCusMeta cusMeta : cusMetas) {
            String tempCusMeta = cusMeta.getCusOrderMeta();
            CusMetaType metaDTO = JacksonUtils.jsonToBean(tempCusMeta, CusMetaType.class);
            if (metaDTO == null) {
                continue;
            }
            pkgMeta.setPkgConfirmMeta(metaDTO.getPkgMeta().getPkgConfirmMeta());
            orderMetaList.add(metaDTO.getOrderMeta());
        }
        responseType.setPkgOrderMeta(pkgMeta);
        responseType.setOrderMetaList(orderMetaList);
        return responseType;
    }

    public List<MetaProcessStrategy> calRealTimeStrategy(Long hotelOrderId) {
        List<MetaProcessStrategy> strategies = new ArrayList<>();
        // 1. 生成订单元数据
        MetaProcessStrategy strategy = new MetaProcessStrategy();
        strategy.setOrderId(hotelOrderId);
        strategy.setDomain(Domain.CONFIRM);

        DataService dataService = DataServiceConfiguration.getBean(DataService.class);
        OSourceCusRelation condition = new OSourceCusRelation();
        condition.setSourceOrderId(hotelOrderId);
        List<OSourceCusRelation> oSourceCusRelations = dataService.selectList(condition);

        List<OXOrder> orders = Lists.newArrayList();
        for (OSourceCusRelation oSourceCusRelation : oSourceCusRelations) {
            OXOrder oxOrder = new OXOrder();
            oxOrder.setOrderId(oSourceCusRelation.getCusOrderId());
            orders.addAll(dataService.selectList(oxOrder));
        }
        // 计算房单对应 action
        List<OXOrder> confirmList = orders.stream().filter(i -> (i.getStatus() & OXOrderEnum.CONFIRMED) == OXOrderEnum.CONFIRMED).collect(Collectors.toList());
        List<OXOrder> failList = orders.stream().filter(i -> (i.getStatus() & OXOrderEnum.CONFIRM_FAIL) == OXOrderEnum.CONFIRM_FAIL).collect(Collectors.toList());
        // 订单全部确认：则主订单 继续处理
        if (confirmList.size() == orders.size()) {
            strategy.setAction("proceed");
        } else if (failList.size() == orders.size()) {
            strategy.setAction("overturn");
        } else {
            // 超时情况下：默认也是推翻
            strategy.setAction("overturn");
        }
        strategies.add(strategy);
        return strategies;
    }


}
