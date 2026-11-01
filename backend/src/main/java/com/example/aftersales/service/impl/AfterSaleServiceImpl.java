package com.example.aftersales.service.impl;

import com.example.aftersales.auth.AuthContext;
import com.example.aftersales.auth.CurrentUser;
import com.example.aftersales.dto.AfterSaleCreateDTO;
import com.example.aftersales.dto.EvidenceSupplementDTO;
import com.example.aftersales.dto.FinalDecisionDTO;
import com.example.aftersales.dto.InspectionDTO;
import com.example.aftersales.dto.ReviewDecisionDTO;
import com.example.aftersales.dto.WarehouseReceiveDTO;
import com.example.aftersales.entity.AfterSaleEntity;
import com.example.aftersales.entity.AfterSaleEventEntity;
import com.example.aftersales.entity.MockOrderEntity;
import com.example.aftersales.entity.OrderItemEntity;
import com.example.aftersales.enums.AfterSaleAction;
import com.example.aftersales.enums.AfterSaleStatus;
import com.example.aftersales.enums.AfterSaleType;
import com.example.aftersales.enums.InspectionResult;
import com.example.aftersales.enums.UserRole;
import com.example.aftersales.exception.ApiException;
import com.example.aftersales.mapper.AfterSaleEventMapper;
import com.example.aftersales.mapper.AfterSaleMapper;
import com.example.aftersales.mapper.OrderMapper;
import com.example.aftersales.mapper.WarehouseRecordMapper;
import com.example.aftersales.service.AfterSaleService;
import com.example.aftersales.vo.AfterSaleView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 售后状态机实现：六个关键业务规则全部落在这里。
 *
 * <pre>
 * 规则 1  只能对本人已完成的模拟订单申请售后        -> create() 里校验订单归属与状态
 * 规则 2  申请数量不能超过可售后数量                -> create() 里用已占用数量算剩余
 * 规则 3  客服同意退回后，仓库才能登记收货和验货    -> receive()/inspect() 校验前置状态
 * 规则 4  验货通过不等于退款或换货完成              -> inspect() 只走到待客服确认
 * 规则 5  拒绝、要求补充、验货异常必须填写原因      -> 对应方法里做非空校验
 * 规则 6  每次状态变化保留岗位、时间、说明          -> writeEvent() 写 after_sale_event
 * 权限    消费者只能看自己的单，岗位不匹配一律 403  -> requireRole() 与 requireOwnership()
 * </pre>
 */
@Service
public class AfterSaleServiceImpl implements AfterSaleService {

    /** 客服工作台关心的状态：轮到自己处理的才进队列。 */
    private static final List<AfterSaleStatus> CS_QUEUE =
            List.of(AfterSaleStatus.PENDING_REVIEW, AfterSaleStatus.INSPECTION_PASSED, AfterSaleStatus.INSPECTION_FAILED);

    /** 仓库工作台关心的状态。 */
    private static final List<AfterSaleStatus> WAREHOUSE_QUEUE =
            List.of(AfterSaleStatus.RETURN_GUIDED, AfterSaleStatus.WAREHOUSE_RECEIVED);

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AfterSaleMapper afterSaleMapper;
    private final AfterSaleEventMapper eventMapper;
    private final WarehouseRecordMapper warehouseRecordMapper;
    private final OrderMapper orderMapper;

    public AfterSaleServiceImpl(AfterSaleMapper afterSaleMapper,
                                AfterSaleEventMapper eventMapper,
                                WarehouseRecordMapper warehouseRecordMapper,
                                OrderMapper orderMapper) {
        this.afterSaleMapper = afterSaleMapper;
        this.eventMapper = eventMapper;
        this.warehouseRecordMapper = warehouseRecordMapper;
        this.orderMapper = orderMapper;
    }

    // ==========================================================
    // 消费者
    // ==========================================================

    /**
     * 发起售后申请。
     *
     * <p>这是整条链路的入口，也是最容易被 AI 或新手写漏的一段：
     * 订单归属、订单是否完成、是否超期、数量是否超限、是否重复申请，
     * 五条校验必须都在写库之前完成。</p>
     */
    @Override
    @Transactional
    public AfterSaleView create(AfterSaleCreateDTO dto) {
        CurrentUser user = AuthContext.require();
        requireRole(user, UserRole.CONSUMER);

        if (dto == null || dto.getOrderItemId() == null) {
            throw ApiException.badRequest("请选择要申请售后的订单商品");
        }
        if (dto.getType() == null) {
            throw ApiException.badRequest("请选择售后类型：退货或换货");
        }
        if (dto.getReasonCode() == null) {
            throw ApiException.badRequest("请选择申请原因");
        }
        if (dto.getQuantity() == null || dto.getQuantity() < 1) {
            throw ApiException.badRequest("申请数量至少为 1 件");
        }

        OrderItemEntity item = orderMapper.findItemById(dto.getOrderItemId());
        if (item == null) {
            throw ApiException.notFound("订单商品不存在：" + dto.getOrderItemId());
        }
        MockOrderEntity order = orderMapper.findOrderById(item.getOrderId());
        if (order == null) {
            throw ApiException.notFound("该商品行对应的订单不存在");
        }

        // 规则 1：只能对本人已完成的模拟订单申请售后
        if (!order.getConsumerId().equals(user.id())) {
            throw ApiException.forbidden("只能对本人已完成的模拟订单申请售后");
        }
        if (!"FINISHED".equals(order.getStatus())) {
            throw ApiException.badRequest("订单 " + order.getOrderNo() + " 尚未完成，不能申请售后");
        }

        // 售后期限：客服受理前先确认商品仍在售后期限内
        if (item.getAfterSaleDeadline() != null && LocalDate.now().isAfter(item.getAfterSaleDeadline())) {
            throw ApiException.badRequest("该商品已超过售后期限（截止 " + item.getAfterSaleDeadline() + "）");
        }

        // 规则 2：申请数量不能超过可售后数量
        int occupied = afterSaleMapper.sumOccupiedQuantity(item.getId());
        int remaining = item.getQuantity() - occupied;
        if (remaining <= 0) {
            throw ApiException.badRequest("该行商品可售后数量为 0（购买 " + item.getQuantity() + " 件，已申请 " + occupied + " 件）");
        }
        if (dto.getQuantity() > remaining) {
            throw ApiException.badRequest("申请数量超过可售后数量，最多还能申请 " + remaining + " 件");
        }

        // 重复申请：同一商品行不允许并存两张进行中的售后单
        AfterSaleEntity inProgress = afterSaleMapper.findInProgressByItemId(item.getId());
        if (inProgress != null) {
            throw ApiException.conflict("该行商品已有进行中的售后单 " + inProgress.getAfterSaleNo()
                    + "（" + inProgress.getStatus().getLabel() + "），请等它处理完再申请");
        }

        AfterSaleEntity entity = dto.toEntity();
        entity.setConsumerId(user.id());
        entity.setAfterSaleNo(generateAfterSaleNo(item.getId()));
        afterSaleMapper.insert(entity);

        writeEvent(entity.getId(), AfterSaleAction.APPLY, null, AfterSaleStatus.PENDING_REVIEW,
                entity.getType().getLabel() + " " + entity.getQuantity() + " 件，原因：" + entity.getReasonCode().getLabel());

        return detail(entity.getId());
    }

    @Override
    @Transactional
    public AfterSaleView supplementEvidence(Long afterSaleId, EvidenceSupplementDTO dto) {
        CurrentUser user = AuthContext.require();
        requireRole(user, UserRole.CONSUMER);

        AfterSaleEntity afterSale = requireAfterSale(afterSaleId);
        requireOwnership(afterSale, user);
        requireStatus(afterSale, AfterSaleStatus.NEED_EVIDENCE);

        String newEvidence = trimToEmpty(dto == null ? null : dto.getEvidenceUrl());
        String newDescription = trimToEmpty(dto == null ? null : dto.getDescription());
        if (newEvidence.isEmpty() && newDescription.isEmpty()) {
            throw ApiException.badRequest("请至少补充清晰的凭证地址或一段说明");
        }

        String evidenceUrl = newEvidence.isEmpty() ? afterSale.getEvidenceUrl() : newEvidence;
        String description = newDescription.isEmpty()
                ? afterSale.getDescription()
                : afterSale.getDescription() + "\n补充说明：" + newDescription;
        afterSaleMapper.updateEvidence(afterSaleId, evidenceUrl, description);
        afterSaleMapper.updateStatus(afterSaleId, AfterSaleStatus.PENDING_REVIEW.name(), null);

        writeEvent(afterSaleId, AfterSaleAction.SUPPLEMENT_EVIDENCE,
                AfterSaleStatus.NEED_EVIDENCE, AfterSaleStatus.PENDING_REVIEW,
                "已补充凭证与说明，重新进入客服审核");

        return detail(afterSaleId);
    }

    @Override
    public List<AfterSaleView> listMine() {
        CurrentUser user = AuthContext.require();
        requireRole(user, UserRole.CONSUMER);
        return decorateAll(afterSaleMapper.listViewsByConsumer(user.id()));
    }

    // ==========================================================
    // 售后客服
    // ==========================================================

    @Override
    public List<AfterSaleView> listCsQueue() {
        CurrentUser user = AuthContext.require();
        requireRole(user, UserRole.CS);
        return decorateAll(afterSaleMapper.listViewsByStatuses(namesOf(CS_QUEUE)));
    }

    @Override
    public List<AfterSaleView> listAll() {
        CurrentUser user = AuthContext.require();
        requireRole(user, UserRole.CS);
        return decorateAll(afterSaleMapper.listAllViews());
    }

    @Override
    @Transactional
    public AfterSaleView review(Long afterSaleId, ReviewDecisionDTO dto) {
        CurrentUser user = AuthContext.require();
        requireRole(user, UserRole.CS);

        AfterSaleEntity afterSale = requireAfterSale(afterSaleId);
        requireStatus(afterSale, AfterSaleStatus.PENDING_REVIEW);

        if (dto == null || dto.getAction() == null) {
            throw ApiException.badRequest("请选择审核动作");
        }
        String reason = trimToEmpty(dto.getReason());

        switch (dto.getAction()) {
            case REQUEST_EVIDENCE -> {
                // 规则 5：要求补充材料必须写原因
                requireReason(reason, "要求补充凭证时必须说明缺少什么材料");
                moveTo(afterSaleId, AfterSaleStatus.NEED_EVIDENCE, null);
                writeEvent(afterSaleId, AfterSaleAction.REQUEST_EVIDENCE,
                        AfterSaleStatus.PENDING_REVIEW, AfterSaleStatus.NEED_EVIDENCE, reason);
            }
            case APPROVE_RETURN -> {
                requireReason(reason, "同意退回时必须填写退回指引，仓库据此收货");
                moveTo(afterSaleId, AfterSaleStatus.RETURN_GUIDED, null);
                writeEvent(afterSaleId, AfterSaleAction.APPROVE_RETURN,
                        AfterSaleStatus.PENDING_REVIEW, AfterSaleStatus.RETURN_GUIDED, reason);
            }
            case REJECT -> {
                // 规则 5：拒绝必须写原因
                requireReason(reason, "拒绝申请时必须说明理由，消费者要据此申诉或补材料");
                moveTo(afterSaleId, AfterSaleStatus.REJECTED, LocalDateTime.now());
                writeEvent(afterSaleId, AfterSaleAction.REJECT,
                        AfterSaleStatus.PENDING_REVIEW, AfterSaleStatus.REJECTED, reason);
            }
            default -> throw ApiException.badRequest("审核阶段只允许：要求补充凭证、同意退回、拒绝申请");
        }

        return detail(afterSaleId);
    }

    /**
     * 客服终定：这一步才真正关闭售后单。
     *
     * <p>验货异常的单子仍然允许客服继续处理，但必须留下结论说明——
     * 行业里叫「售后争议的岗位兜底」，系统里就是这段校验。</p>
     */
    @Override
    @Transactional
    public AfterSaleView finalizeDecision(Long afterSaleId, FinalDecisionDTO dto) {
        CurrentUser user = AuthContext.require();
        requireRole(user, UserRole.CS);

        AfterSaleEntity afterSale = requireAfterSale(afterSaleId);
        if (afterSale.getStatus() != AfterSaleStatus.INSPECTION_PASSED
                && afterSale.getStatus() != AfterSaleStatus.INSPECTION_FAILED) {
            throw conflictByStatus(afterSale, "只有仓库给出验货结论后，客服才能确认换货或退款");
        }
        if (dto == null || dto.getAction() == null) {
            throw ApiException.badRequest("请选择处理结论");
        }

        boolean inspectionFailed = afterSale.getStatus() == AfterSaleStatus.INSPECTION_FAILED;
        String note = trimToEmpty(dto.getNote());
        if (inspectionFailed) {
            // 规则 5 的延伸：验货异常后的任何结论都要写清楚依据
            requireReason(note, "验货异常后的处理结论必须填写说明");
        }

        OrderItemEntity item = orderMapper.findItemById(afterSale.getOrderItemId());
        BigDecimal upperBound = item.getUnitPrice().multiply(BigDecimal.valueOf(afterSale.getQuantity()));

        switch (dto.getAction()) {
            case REJECT -> {
                requireReason(note, "拒绝申请时必须说明理由");
                moveTo(afterSaleId, AfterSaleStatus.REJECTED, LocalDateTime.now());
                writeFinalNote(afterSaleId, note, user);
                writeEvent(afterSaleId, AfterSaleAction.REJECT,
                        afterSale.getStatus(), AfterSaleStatus.REJECTED, note);
            }
            case CONFIRM_EXCHANGE -> {
                if (afterSale.getType() != AfterSaleType.EXCHANGE) {
                    throw ApiException.badRequest("这是一张退货申请，不能确认换货；请改用确认退款或拒绝");
                }
                String reissueNo = trimToEmpty(dto.getReissueNo());
                if (reissueNo.isEmpty()) {
                    throw ApiException.badRequest("换货完成前必须记录模拟补发单号");
                }
                moveTo(afterSaleId, AfterSaleStatus.EXCHANGE_DONE, LocalDateTime.now());
                warehouseRecordMapper.updateFinalization(afterSaleId, reissueNo, null, "",
                        user.id(), LocalDateTime.now(), note);
                writeEvent(afterSaleId, AfterSaleAction.CONFIRM_EXCHANGE, afterSale.getStatus(), AfterSaleStatus.EXCHANGE_DONE,
                        "模拟补发单号：" + reissueNo + (note.isEmpty() ? "" : "；结论：" + note));
            }
            case CONFIRM_REFUND -> {
                if (afterSale.getType() != AfterSaleType.RETURN) {
                    throw ApiException.badRequest("这是一张换货申请，首版只支持确认换货；如需退款请先关闭再另建退货单");
                }
                BigDecimal amount = dto.getRefundAmount();
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw ApiException.badRequest("请填写大于 0 的模拟退款金额");
                }
                if (amount.compareTo(upperBound) > 0) {
                    throw ApiException.badRequest("模拟退款金额不能超过该行商品成交金额 " + upperBound + " 元");
                }
                moveTo(afterSaleId, AfterSaleStatus.REFUND_DONE, LocalDateTime.now());
                warehouseRecordMapper.updateFinalization(afterSaleId, "", amount, "MOCK_SUCCESS",
                        user.id(), LocalDateTime.now(), note);
                writeEvent(afterSaleId, AfterSaleAction.CONFIRM_REFUND, afterSale.getStatus(), AfterSaleStatus.REFUND_DONE,
                        "模拟退款 " + amount + " 元（不接真实资金）" + (note.isEmpty() ? "" : "；结论：" + note));
            }
            default -> throw ApiException.badRequest("终定阶段只允许：确认换货、确认退款、拒绝申请");
        }

        return detail(afterSaleId);
    }

    // ==========================================================
    // 仓库人员
    // ==========================================================

    @Override
    public List<AfterSaleView> listWarehouseQueue() {
        CurrentUser user = AuthContext.require();
        requireRole(user, UserRole.WAREHOUSE);
        return decorateAll(afterSaleMapper.listViewsByStatuses(namesOf(WAREHOUSE_QUEUE)));
    }

    @Override
    @Transactional
    public AfterSaleView receive(Long afterSaleId, WarehouseReceiveDTO dto) {
        CurrentUser user = AuthContext.require();
        requireRole(user, UserRole.WAREHOUSE);

        AfterSaleEntity afterSale = requireAfterSale(afterSaleId);
        // 规则 3：客服没同意退回，仓库不能提前收货
        if (afterSale.getStatus() != AfterSaleStatus.RETURN_GUIDED) {
            throw conflictByStatus(afterSale, "客服还未同意退回，仓库不能提前登记收货");
        }

        LocalDateTime now = LocalDateTime.now();
        warehouseRecordMapper.insertReceived(afterSaleId, user.id(), now);
        afterSaleMapper.updateStatus(afterSaleId, AfterSaleStatus.WAREHOUSE_RECEIVED.name(), null);

        String note = trimToEmpty(dto == null ? null : dto.getNote());
        writeEvent(afterSaleId, AfterSaleAction.RECEIVE,
                AfterSaleStatus.RETURN_GUIDED, AfterSaleStatus.WAREHOUSE_RECEIVED,
                note.isEmpty() ? "已签收退回包裹" : note);

        return detail(afterSaleId);
    }

    /**
     * 登记验货结果。
     *
     * <p>规则 4：验货通过只把单据推进到「待客服确认」，
     * 换货和退款由客服在 finalizeDecision() 里关闭。</p>
     */
    @Override
    @Transactional
    public AfterSaleView inspect(Long afterSaleId, InspectionDTO dto) {
        CurrentUser user = AuthContext.require();
        requireRole(user, UserRole.WAREHOUSE);

        AfterSaleEntity afterSale = requireAfterSale(afterSaleId);
        requireStatus(afterSale, AfterSaleStatus.WAREHOUSE_RECEIVED);

        if (dto == null || dto.getResult() == null) {
            throw ApiException.badRequest("请选择验货结果：通过或异常");
        }
        if (dto.getResult() == InspectionResult.PENDING) {
            throw ApiException.badRequest("验货结果只能是「通过」或「异常」，待验货是登记收货时的默认状态");
        }

        String abnormalReason = trimToEmpty(dto.getAbnormalReason());
        if (dto.getResult() == InspectionResult.ABNORMAL && abnormalReason.isEmpty()) {
            // 规则 5：验货异常必须填写原因
            throw ApiException.badRequest("验货异常必须填写原因，客服要据此判断是否继续处理");
        }

        AfterSaleStatus next = dto.getResult() == InspectionResult.PASSED
                ? AfterSaleStatus.INSPECTION_PASSED
                : AfterSaleStatus.INSPECTION_FAILED;

        LocalDateTime now = LocalDateTime.now();
        warehouseRecordMapper.updateInspection(afterSaleId, dto.getResult().name(), user.id(), now,
                dto.getResult() == InspectionResult.PASSED ? "" : abnormalReason);
        afterSaleMapper.updateStatus(afterSaleId, next.name(), null);

        writeEvent(afterSaleId, AfterSaleAction.INSPECT, afterSale.getStatus(), next,
                dto.getResult().getLabel() + (abnormalReason.isEmpty() ? "" : "：" + abnormalReason));

        return detail(afterSaleId);
    }

    // ==========================================================
    // 共用
    // ==========================================================

    @Override
    public AfterSaleView detail(Long afterSaleId) {
        CurrentUser user = AuthContext.require();

        AfterSaleView view = afterSaleMapper.findViewById(afterSaleId);
        if (view == null) {
            throw ApiException.notFound("售后单不存在：" + afterSaleId);
        }
        // 规则 6（权限）：普通消费者不能查看他人订单和售后资料
        if (user.isConsumer() && !view.getConsumerId().equals(user.id())) {
            throw ApiException.forbidden("消费者只能查看自己的订单和售后资料，无权查看 " + view.getAfterSaleNo());
        }

        decorate(view);
        view.setTimeline(eventMapper.listByAfterSaleId(afterSaleId));
        view.setWarehouse(warehouseRecordMapper.findByAfterSaleId(afterSaleId));

        int occupied = afterSaleMapper.sumOccupiedQuantity(view.getOrderItemId());
        view.setRemainingQuantity(Math.max((view.getItemQuantity() == null ? 0 : view.getItemQuantity()) - occupied, 0));
        return view;
    }

    // ==========================================================
    // 校验与填充的私有工具
    // ==========================================================

    private AfterSaleEntity requireAfterSale(Long afterSaleId) {
        AfterSaleEntity afterSale = afterSaleMapper.findById(afterSaleId);
        if (afterSale == null) {
            throw ApiException.notFound("售后单不存在：" + afterSaleId);
        }
        return afterSale;
    }

    /**
     * 岗位校验：越权一律 403，错误信息里说清楚该由谁处理。
     */
    private void requireRole(CurrentUser user, UserRole expected) {
        if (user.role() != expected) {
            throw ApiException.forbidden("这一步应由「" + expected.getLabel() + "」处理，当前身份是「" + user.role().getLabel() + "」");
        }
    }

    private void requireOwnership(AfterSaleEntity afterSale, CurrentUser user) {
        if (!afterSale.getConsumerId().equals(user.id())) {
            throw ApiException.forbidden("这是别人的售后单，消费者只能处理自己发起的申请");
        }
    }

    private void requireStatus(AfterSaleEntity afterSale, AfterSaleStatus expected) {
        if (afterSale.getStatus() != expected) {
            throw conflictByStatus(afterSale, "这一步要求单据处于「" + expected.getLabel() + "」");
        }
    }

    private ApiException conflictByStatus(AfterSaleEntity afterSale, String hint) {
        return ApiException.conflict(hint + "，当前状态是「" + afterSale.getStatus().getLabel() + "」");
    }

    private void moveTo(Long afterSaleId, AfterSaleStatus status, LocalDateTime closedAt) {
        afterSaleMapper.updateStatus(afterSaleId, status.name(), closedAt);
    }

    private void writeFinalNote(Long afterSaleId, String note, CurrentUser user) {
        warehouseRecordMapper.updateFinalization(afterSaleId, "", null, "", user.id(), LocalDateTime.now(), note);
    }

    private void requireReason(String reason, String message) {
        if (reason == null || reason.trim().isEmpty()) {
            throw ApiException.badRequest(message);
        }
    }

    /**
     * 写时间线：岗位、动作、变化前后状态、说明、时间。
     */
    private void writeEvent(Long afterSaleId, AfterSaleAction action,
                           AfterSaleStatus from, AfterSaleStatus to, String note) {
        CurrentUser user = AuthContext.require();
        AfterSaleEventEntity event = new AfterSaleEventEntity();
        event.setAfterSaleId(afterSaleId);
        event.setOperatorId(user.id());
        event.setOperatorRole(user.role().name());
        event.setOperatorName(user.displayName());
        event.setAction(action.getLabel());
        event.setFromStatus(from == null ? "" : from.name());
        event.setToStatus(to == null ? "" : to.name());
        event.setNote(note == null ? "" : note);
        eventMapper.insert(event);
    }

    private List<AfterSaleView> decorateAll(List<AfterSaleView> views) {
        views.forEach(this::decorate);
        return views;
    }

    /**
     * 把枚举翻译成中文标签，并算出当前操作人能点的按钮。
     *
     * <p>按钮由后端决定，前端只渲染：这样「页面上有这个按钮」就等于「后端确认允许这个操作」，
     * 两边不会各写一套判断然后打架。</p>
     */
    private void decorate(AfterSaleView view) {
        AfterSaleStatus status = view.getStatus();
        view.setStatusLabel(status.getLabel());
        view.setTypeLabel(view.getType() == null ? "" : view.getType().getLabel());
        view.setReasonLabel(view.getReasonCode() == null ? "" : view.getReasonCode().getLabel());
        view.setOwnerRoleLabel(status.getOwnerRole() == null ? "已关闭" : status.getOwnerRole().getLabel());
        view.setTerminal(status.isTerminal());
        view.setAllowedActions(allowedActions(view, status));
    }

    private List<AfterSaleView.ActionOption> allowedActions(AfterSaleView view, AfterSaleStatus status) {
        List<AfterSaleView.ActionOption> options = new ArrayList<>();
        if (status.isTerminal()) {
            return options;
        }

        CurrentUser user = AuthContext.require();
        boolean ownerConsumer = user.isConsumer() && view.getConsumerId().equals(user.id());

        if (user.role() == UserRole.CONSUMER) {
            if (ownerConsumer && status == AfterSaleStatus.NEED_EVIDENCE) {
                options.add(AfterSaleView.ActionOption.of(AfterSaleAction.SUPPLEMENT_EVIDENCE));
            }
            return options;
        }

        if (user.role() == UserRole.CS) {
            switch (status) {
                case PENDING_REVIEW -> options.addAll(List.of(
                        AfterSaleView.ActionOption.of(AfterSaleAction.REQUEST_EVIDENCE),
                        AfterSaleView.ActionOption.of(AfterSaleAction.APPROVE_RETURN),
                        AfterSaleView.ActionOption.of(AfterSaleAction.REJECT)));
                case INSPECTION_PASSED, INSPECTION_FAILED -> {
                    AfterSaleAction main = view.getType() == AfterSaleType.EXCHANGE
                            ? AfterSaleAction.CONFIRM_EXCHANGE
                            : AfterSaleAction.CONFIRM_REFUND;
                    options.add(AfterSaleView.ActionOption.of(main));
                    options.add(AfterSaleView.ActionOption.of(AfterSaleAction.REJECT));
                }
                default -> {
                    // NEED_EVIDENCE / 仓库环节：客服没有可做的动作
                }
            }
            return options;
        }

        if (user.role() == UserRole.WAREHOUSE) {
            if (status == AfterSaleStatus.RETURN_GUIDED) {
                options.add(AfterSaleView.ActionOption.of(AfterSaleAction.RECEIVE));
            } else if (status == AfterSaleStatus.WAREHOUSE_RECEIVED) {
                options.add(AfterSaleView.ActionOption.of(AfterSaleAction.INSPECT));
            }
        }
        return options;
    }

    private List<String> namesOf(List<AfterSaleStatus> statuses) {
        return statuses.stream().map(Enum::name).toList();
    }

    /**
     * 单号规则：AS + 时间戳 + 商品行号。
     * 演示环境数据量小够用；生产应换成发号器或数据库序列，已写进 README 的已知限制。
     */
    private String generateAfterSaleNo(Long orderItemId) {
        return "AS" + NO_FORMATTER.format(LocalDateTime.now()) + "-" + orderItemId;
    }

    private String trimToEmpty(String text) {
        return text == null ? "" : text.trim();
    }
}
