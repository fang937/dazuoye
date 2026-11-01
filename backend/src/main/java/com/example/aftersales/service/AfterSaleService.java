package com.example.aftersales.service;

import com.example.aftersales.dto.AfterSaleCreateDTO;
import com.example.aftersales.dto.EvidenceSupplementDTO;
import com.example.aftersales.dto.FinalDecisionDTO;
import com.example.aftersales.dto.InspectionDTO;
import com.example.aftersales.dto.ReviewDecisionDTO;
import com.example.aftersales.dto.WarehouseReceiveDTO;
import com.example.aftersales.vo.AfterSaleView;

import java.util.List;

/**
 * 售后单业务服务：本项目全部业务规则和状态机都集中在这一层。
 *
 * <p>每个方法都做三件事：确认操作人岗位、确认单据当前状态允许该动作、
 * 改状态并写一条时间线。Controller 和前端都不重复实现这些判断。</p>
 */
public interface AfterSaleService {

    // ---- 消费者 ----

    /** 发起退货或换货申请，进入「待客服审核」。 */
    AfterSaleView create(AfterSaleCreateDTO dto);

    /** 被客服退回补充凭证后，重新提交凭证并回到审核。 */
    AfterSaleView supplementEvidence(Long afterSaleId, EvidenceSupplementDTO dto);

    /** 消费者工作台：只看自己的售后单。 */
    List<AfterSaleView> listMine();

    // ---- 售后客服 ----

    /** 客服工作台队列：待审核 + 验货通过待确认 + 验货异常待处理。 */
    List<AfterSaleView> listCsQueue();

    /** 客服查看全部售后单，用于统计与争议处理。 */
    List<AfterSaleView> listAll();

    /** 审核：要求补充凭证 / 同意退回并给出指引 / 拒绝。 */
    AfterSaleView review(Long afterSaleId, ReviewDecisionDTO dto);

    /** 终定：确认换货（记录补发单号）/ 确认退款（记录模拟金额）/ 验货异常后拒绝。 */
    AfterSaleView finalizeDecision(Long afterSaleId, FinalDecisionDTO dto);

    // ---- 仓库人员 ----

    /** 仓库工作台队列：待收货 + 已收货待验货。 */
    List<AfterSaleView> listWarehouseQueue();

    /** 登记收到退回包裹，前置条件是客服已经同意退回。 */
    AfterSaleView receive(Long afterSaleId, WarehouseReceiveDTO dto);

    /** 登记验货结果；异常必须写原因，且不会直接关闭售后单。 */
    AfterSaleView inspect(Long afterSaleId, InspectionDTO dto);

    // ---- 三个岗位共用 ----

    /** 单据详情：含时间线和仓库记录；消费者只能看自己的。 */
    AfterSaleView detail(Long afterSaleId);
}
