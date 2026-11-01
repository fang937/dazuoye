package com.example.aftersales.vo;

import com.example.aftersales.entity.AfterSaleEventEntity;
import com.example.aftersales.entity.WarehouseRecordEntity;
import com.example.aftersales.enums.AfterSaleAction;
import com.example.aftersales.enums.AfterSaleReason;
import com.example.aftersales.enums.AfterSaleStatus;
import com.example.aftersales.enums.AfterSaleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后单视图对象：列表和详情共用一个结构，详情才填充时间线与仓库记录。
 *
 * <p>SQL 通过多表连接把「谁的单、哪件商品、现在到哪一步」一次取出来；
 * Service 再补上中文标签和当前操作人能做的动作。前端的按钮完全由 allowedActions 渲染，
 * 所以页面上出现某个按钮，就代表后端确认这个岗位在这一步确实能做这件事。</p>
 */
@Data
public class AfterSaleView {

    // ---- after_sale 表自身字段 ----
    private Long id;
    private String afterSaleNo;
    private AfterSaleType type;
    private AfterSaleReason reasonCode;
    private Integer quantity;
    private String description;
    private String evidenceUrl;
    private AfterSaleStatus status;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---- 关联出来的业务上下文 ----
    private Long consumerId;
    private String consumerName;
    private String orderNo;
    private Long orderItemId;
    private String productName;
    private String spec;
    private BigDecimal unitPrice;
    private Integer itemQuantity;
    private LocalDate afterSaleDeadline;

    // ---- Service 填充的展示结论 ----
    private String statusLabel;
    private String typeLabel;
    private String reasonLabel;
    /** 当前轮到哪个岗位处理，终态为空。 */
    private String ownerRoleLabel;
    private Boolean terminal;
    private Integer remainingQuantity;
    private List<ActionOption> allowedActions;

    // ---- 仅详情接口填充 ----
    private List<AfterSaleEventEntity> timeline;
    private WarehouseRecordEntity warehouse;

    /**
     * 按钮描述：name 提交回后端，label 显示给人看。
     */
    public record ActionOption(String name, String label) {

        public static ActionOption of(AfterSaleAction action) {
            return new ActionOption(action.name(), action.getLabel());
        }
    }
}
