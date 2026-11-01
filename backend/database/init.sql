-- ============================================================
-- 服装网店退换货处理系统 · MySQL 初始化脚本
--
-- 执行方式：
--   方式 1（终端导入）：mysql -u root -p < backend/database/init.sql
--   方式 2（客户端内）：source D:/.../after_sale_demo/backend/database/init.sql;
--
-- 脚本采用 DROP + CREATE，可反复执行，保证「从新环境重新拉取即可复现演示数据」。
-- 所有日期使用 NOW() 相对推算，不会随时间过期。
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS aftersales_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE aftersales_demo;

SET NAMES utf8mb4;

DROP TABLE IF EXISTS after_sale_event;
DROP TABLE IF EXISTS warehouse_record;
DROP TABLE IF EXISTS after_sale;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS mock_order;
DROP TABLE IF EXISTS sys_user;

-- ------------------------------------------------------------
-- 1. 账号表：登录用，登录后拿到令牌，后续请求带 Authorization: Bearer <token>
--    演示项目密码明文存储（统一 123456），真实系统必须换成 BCrypt 等慢哈希
-- ------------------------------------------------------------
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户自增主键',
    username VARCHAR(40) NOT NULL UNIQUE COMMENT '登录账号',
    password VARCHAR(60) NOT NULL COMMENT '登录密码（演示用明文，生产必须哈希）',
    display_name VARCHAR(40) NOT NULL COMMENT '工作台上显示的人名',
    role VARCHAR(20) NOT NULL COMMENT '岗位：CONSUMER 消费者 / CS 售后客服 / WAREHOUSE 仓库人员',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号与岗位';

-- ------------------------------------------------------------
-- 2. 模拟订单表：系统不开发下单支付，只做已完成的订单数据
-- ------------------------------------------------------------
CREATE TABLE mock_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单自增主键',
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
    consumer_id BIGINT NOT NULL COMMENT '下单消费者，对应 sys_user.id',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单金额（模拟）',
    placed_at DATETIME NOT NULL COMMENT '下单时间',
    completed_at DATETIME NOT NULL COMMENT '确认收货时间，售后期限从这天算',
    status VARCHAR(20) NOT NULL DEFAULT 'FINISHED' COMMENT '演示数据一律为已完成订单',
    KEY idx_order_consumer (consumer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟订单';

-- ------------------------------------------------------------
-- 3. 订单商品行表：售后申请针对的是某一行商品
-- ------------------------------------------------------------
CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品行自增主键',
    order_id BIGINT NOT NULL COMMENT '所属订单',
    product_name VARCHAR(80) NOT NULL COMMENT '商品名称',
    spec VARCHAR(40) NOT NULL DEFAULT '' COMMENT '规格：颜色/尺码',
    unit_price DECIMAL(10, 2) NOT NULL COMMENT '成交单价',
    quantity INT NOT NULL COMMENT '购买数量，决定最大可售后数量',
    after_sale_deadline DATE NOT NULL COMMENT '售后申请截止日期',
    KEY idx_item_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品行';

-- ------------------------------------------------------------
-- 4. 售后单表：核心业务单据，status 就是状态机所在地
-- ------------------------------------------------------------
CREATE TABLE after_sale (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '售后单自增主键',
    after_sale_no VARCHAR(32) NOT NULL UNIQUE COMMENT '售后单号',
    order_item_id BIGINT NOT NULL COMMENT '针对哪一行订单商品',
    consumer_id BIGINT NOT NULL COMMENT '发起人，决定归属与可见范围',
    type VARCHAR(20) NOT NULL COMMENT 'RETURN 退货 / EXCHANGE 换货',
    reason_code VARCHAR(30) NOT NULL COMMENT '申请原因编码：尺码不合适、破损等',
    quantity INT NOT NULL COMMENT '申请数量，不能超过可售后数量',
    description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '消费者说明',
    evidence_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '图片凭证地址（模拟路径，不上传真实文件）',
    status VARCHAR(30) NOT NULL COMMENT '状态机：见 AfterSaleStatus 枚举',
    closed_at DATETIME NULL COMMENT '进入终态的时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近变化时间',
    KEY idx_after_sale_consumer (consumer_id),
    KEY idx_after_sale_status (status),
    KEY idx_after_sale_item (order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后单';

-- ------------------------------------------------------------
-- 5. 仓库记录表：收货、验货、补发单号、模拟退款分开保存
--    「验货通过」和「售后完成」是两个不同的业务事实，所以不塞进 after_sale 一张表
-- ------------------------------------------------------------
CREATE TABLE warehouse_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '仓库记录自增主键',
    after_sale_id BIGINT NOT NULL UNIQUE COMMENT '对应的售后单',
    received_by BIGINT NULL COMMENT '登记收货的仓库账号',
    received_at DATETIME NULL COMMENT '收货时间',
    inspection_result VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待验货 / PASSED 通过 / ABNORMAL 异常',
    inspector_id BIGINT NULL COMMENT '验货人',
    inspected_at DATETIME NULL COMMENT '验货时间',
    abnormal_reason VARCHAR(300) NOT NULL DEFAULT '' COMMENT '验货异常原因，必填',
    reissue_no VARCHAR(40) NOT NULL DEFAULT '' COMMENT '换货模拟补发单号',
    refund_amount DECIMAL(10, 2) NULL COMMENT '模拟退款金额，不接真实资金',
    refund_result VARCHAR(20) NOT NULL DEFAULT '' COMMENT 'MOCK_SUCCESS：模拟退款结果',
    operator_id BIGINT NULL COMMENT '终定的客服账号',
    finalized_at DATETIME NULL COMMENT '客服确认时间',
    final_note VARCHAR(300) NOT NULL DEFAULT '' COMMENT '客服结论说明'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库收货验货与售后结论记录';

-- ------------------------------------------------------------
-- 6. 售后时间线表：每次状态变化都留下岗位、动作、说明和时间
-- ------------------------------------------------------------
CREATE TABLE after_sale_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '事件自增主键',
    after_sale_id BIGINT NOT NULL COMMENT '所属售后单',
    operator_id BIGINT NULL COMMENT '操作人账号',
    operator_role VARCHAR(20) NOT NULL COMMENT '操作岗位',
    operator_name VARCHAR(40) NOT NULL COMMENT '操作人显示名，冗余存储便于查看历史',
    action VARCHAR(60) NOT NULL COMMENT '动作名称，例如「要求补充凭证」',
    from_status VARCHAR(30) NOT NULL DEFAULT '' COMMENT '变化前状态',
    to_status VARCHAR(30) NOT NULL DEFAULT '' COMMENT '变化后状态',
    note VARCHAR(500) NOT NULL DEFAULT '' COMMENT '说明或原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    KEY idx_event_after_sale (after_sale_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后操作时间线';

-- ============================================================
-- 演示数据
-- ============================================================

-- 1) 演示账号（密码统一 123456，登录页有一键填入）
INSERT INTO sys_user (id, username, password, display_name, role) VALUES
(1, 'lin',       '123456', '小林（消费者）',   'CONSUMER'),
(2, 'hong',      '123456', '小红（消费者）',   'CONSUMER'),
(3, 'kefu_wang', '123456', '王敏（售后客服）', 'CS'),
(4, 'wh_li',     '123456', '李强（仓库人员）', 'WAREHOUSE');

-- 2) 模拟订单：小林的订单已完成 5 天，仍在 15 天售后期内
INSERT INTO mock_order (id, order_no, consumer_id, total_amount, placed_at, completed_at, status) VALUES
(1, 'NO20261001001', 1, 715.00, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 'FINISHED'),
(2, 'NO20261001002', 1, 199.00, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 55 DAY), 'FINISHED'),
(3, 'NO20261001003', 2, 468.00, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 'FINISHED');

-- 3) 订单商品行：售后期限 = 确认收货 + 15 天；订单 2 已超期；商品行 6 完全干净，用来演示从零走完整条链路
INSERT INTO order_item (id, order_id, product_name, spec, unit_price, quantity, after_sale_deadline) VALUES
(1, 1, '纯棉牛津纺长袖衬衫', '白色 / M', 179.00, 2, DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 15 DAY)),
(2, 1, '免烫商务衬衫', '浅蓝 / L', 179.00, 1, DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 15 DAY)),
(3, 2, '春秋薄款针织开衫', '米色 / F', 199.00, 1, DATE_ADD(DATE_SUB(NOW(), INTERVAL 55 DAY), INTERVAL 15 DAY)),
(4, 3, '法式方领泡泡袖衬衫', '杏色 / S', 229.00, 2, DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 15 DAY)),
(5, 3, '直筒休闲西裤', '深灰 / 30', 239.00, 1, DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 15 DAY)),
(6, 1, '纯色圆领纯棉短袖T恤', '白色 / L', 89.00, 2, DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 15 DAY));

-- 4) 售后单：覆盖各个状态，方便三个岗位一进去就有活可干
INSERT INTO after_sale (id, after_sale_no, order_item_id, consumer_id, type, reason_code, quantity, description, evidence_url, status, closed_at, created_at) VALUES
-- 小林的换货申请，正在等客服审核
(1, 'AS20261016001', 1, 1, 'EXCHANGE', 'SIZE_MISMATCH', 1, 'M 码上身偏小，希望换成 L 码，吊牌还在。', '/mock/evidence/shirt-m.jpg', 'PENDING_REVIEW', NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
-- 小红的退货申请，凭证看不清，被客服退回补充
(2, 'AS20261016002', 5, 2, 'RETURN', 'DAMAGED', 1, '裤子有污迹，照片在附件里。', '/mock/evidence/pants-blur.jpg', 'NEED_EVIDENCE', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 小林的退货申请，客服已同意并给出退回指引，等仓库收货
(3, 'AS20261016003', 2, 1, 'RETURN', 'NO_LONGER_NEED', 1, '颜色与页面差别较大，不想要了，整件退回。', '/mock/evidence/blue-shirt.jpg', 'RETURN_GUIDED', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- 小红的换货申请，仓库已验货通过，等客服确认补发
(4, 'AS20261016004', 4, 2, 'EXCHANGE', 'WRONG_ITEM', 1, '发来的方领款式不对，我下单的是翻领。', '/mock/evidence/wrong-collar.jpg', 'INSPECTION_PASSED', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- 小红上一轮的换货申请被拒绝，已是终态，用于展示已关闭单据
(5, 'AS20261015005', 4, 2, 'EXCHANGE', 'QUALITY_ISSUE', 1, '领口线头较多，希望换新。', '', 'REJECTED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY));

-- 5) 仓库记录：只有已经走到仓库环节的单才有；AS3 停在「待仓库收货」，所以故意没有记录
INSERT INTO warehouse_record (id, after_sale_id, received_by, received_at, inspection_result, inspector_id, inspected_at, abnormal_reason, reissue_no, refund_amount, refund_result, operator_id, finalized_at, final_note) VALUES
(2, 4, 4, DATE_SUB(NOW(), INTERVAL 2 DAY), 'PASSED', 4, DATE_SUB(NOW(), INTERVAL 1 DAY), '', '', NULL, '', NULL, NULL, '');

-- 6) 时间线：每张单据都能看到谁在什么时候做了什么
INSERT INTO after_sale_event (after_sale_id, operator_id, operator_role, operator_name, action, from_status, to_status, note, created_at) VALUES
(1, 1, 'CONSUMER', '小林（消费者）', '提交售后申请', '', 'PENDING_REVIEW', '换货 1 件，原因：尺码不合适', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(2, 2, 'CONSUMER', '小红（消费者）', '提交售后申请', '', 'PENDING_REVIEW', '退货 1 件，原因：商品破损', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 3, 'CS', '王敏（售后客服）', '要求补充凭证', 'PENDING_REVIEW', 'NEED_EVIDENCE', '污迹照片模糊，请补充清晰近照与吊牌照片', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(3, 2, 'CONSUMER', '小林（消费者）', '提交售后申请', '', 'PENDING_REVIEW', '退货 1 件，原因：与页面颜色不符', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 3, 'CS', '王敏（售后客服）', '同意退回并给出售后指引', 'PENDING_REVIEW', 'RETURN_GUIDED', '请退回至杭州余杭仓，收件人：售后组，包装保持吊牌完整', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 2, 'CONSUMER', '小红（消费者）', '提交售后申请', '', 'PENDING_REVIEW', '换货 1 件，原因：发错商品', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4, 3, 'CS', '王敏（售后客服）', '同意退回并给出售后指引', 'PENDING_REVIEW', 'RETURN_GUIDED', '错发商品请一并寄回，我们将补发正确款式', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 4, 'WAREHOUSE', '李强（仓库人员）', '登记收货', 'RETURN_GUIDED', 'WAREHOUSE_RECEIVED', '包裹已签收，外包装完整', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 4, 'WAREHOUSE', '李强（仓库人员）', '验货通过', 'WAREHOUSE_RECEIVED', 'INSPECTION_PASSED', '退回商品与申请一致，未穿着、吊牌完整', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 2, 'CONSUMER', '小红（消费者）', '提交售后申请', '', 'PENDING_REVIEW', '换货 1 件，原因：质量问题', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(5, 3, 'CS', '王敏（售后客服）', '拒绝申请', 'PENDING_REVIEW', 'REJECTED', '线头属于正常工艺范围，未超过售后期限内的质量问题标准', DATE_SUB(NOW(), INTERVAL 2 DAY));
