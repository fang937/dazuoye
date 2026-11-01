package com.example.aftersales.controller;

import com.example.aftersales.dto.AfterSaleCreateDTO;
import com.example.aftersales.dto.EvidenceSupplementDTO;
import com.example.aftersales.dto.FinalDecisionDTO;
import com.example.aftersales.dto.InspectionDTO;
import com.example.aftersales.dto.ReviewDecisionDTO;
import com.example.aftersales.dto.WarehouseReceiveDTO;
import com.example.aftersales.service.AfterSaleService;
import com.example.aftersales.vo.AfterSaleView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 售后单 HTTP 接口。
 *
 * <p>Controller 只做「收请求 → 调 Service → 回 JSON」，不写业务判断、不拼 SQL。
 * 三个岗位的工作台各自调自己的队列接口，权限与状态校验都在 Service 里。</p>
 *
 * <p>跨域配置集中在 {@link com.example.aftersales.config.WebConfig}，
 * 所以这里不再逐个类加 {@code @CrossOrigin}。</p>
 */
@RestController
@RequestMapping("/api/after-sales")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    public AfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    // ---------- 查询 ----------

    /** GET /api/after-sales/mine：消费者工作台，只返回本人发起的售后单。 */
    @GetMapping("/mine")
    public List<AfterSaleView> mine() {
        return afterSaleService.listMine();
    }

    /** GET /api/after-sales/cs-queue：客服待处理队列（待审核 + 待终定 + 验货异常）。 */
    @GetMapping("/cs-queue")
    public List<AfterSaleView> csQueue() {
        return afterSaleService.listCsQueue();
    }

    /** GET /api/after-sales/warehouse-queue：仓库待处理队列（待收货 + 待验货）。 */
    @GetMapping("/warehouse-queue")
    public List<AfterSaleView> warehouseQueue() {
        return afterSaleService.listWarehouseQueue();
    }

    /** GET /api/after-sales/all：仅客服可用，用于查看全部单据与争议追溯。 */
    @GetMapping("/all")
    public List<AfterSaleView> all() {
        return afterSaleService.listAll();
    }

    /** GET /api/after-sales/{id}：详情，含完整时间线和仓库记录。 */
    @GetMapping("/{id}")
    public AfterSaleView detail(@PathVariable Long id) {
        return afterSaleService.detail(id);
    }

    // ---------- 消费者动作 ----------

    /**
     * POST /api/after-sales：发起退货或换货申请。
     *
     * <p>请求体对应 {@link AfterSaleCreateDTO}：订单商品行、类型、原因、数量、说明、凭证。
     * 单号与初始状态由后端生成，前端传了也不认。</p>
     */
    @PostMapping
    public ResponseEntity<AfterSaleView> create(@RequestBody AfterSaleCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(afterSaleService.create(dto));
    }

    /** POST /api/after-sales/{id}/evidence：被退回后补充凭证，重新进入审核。 */
    @PostMapping("/{id}/evidence")
    public AfterSaleView supplement(@PathVariable Long id, @RequestBody EvidenceSupplementDTO dto) {
        return afterSaleService.supplementEvidence(id, dto);
    }

    // ---------- 客服动作 ----------

    /** POST /api/after-sales/{id}/review：要求补充凭证 / 同意退回 / 拒绝。 */
    @PostMapping("/{id}/review")
    public AfterSaleView review(@PathVariable Long id, @RequestBody ReviewDecisionDTO dto) {
        return afterSaleService.review(id, dto);
    }

    /** POST /api/after-sales/{id}/finalize：确认换货（补发单号）/ 确认退款（模拟金额）/ 拒绝。 */
    @PostMapping("/{id}/finalize")
    public AfterSaleView finalize(@PathVariable Long id, @RequestBody FinalDecisionDTO dto) {
        return afterSaleService.finalizeDecision(id, dto);
    }

    // ---------- 仓库动作 ----------

    /** POST /api/after-sales/{id}/receive：登记收到退回包裹，前提是客服已同意退回。 */
    @PostMapping("/{id}/receive")
    public AfterSaleView receive(@PathVariable Long id, @RequestBody(required = false) WarehouseReceiveDTO dto) {
        return afterSaleService.receive(id, dto);
    }

    /** POST /api/after-sales/{id}/inspect：登记验货结果，异常必须写原因。 */
    @PostMapping("/{id}/inspect")
    public AfterSaleView inspect(@PathVariable Long id, @RequestBody InspectionDTO dto) {
        return afterSaleService.inspect(id, dto);
    }
}
