/**
 * 动作与表单的对应关系。
 *
 * 后端在每个售后单上返回 allowedActions（这个身份此刻能做什么），
 * 页面只负责按动作名渲染对应字段并提交到对应接口，
 * 判断「能不能做」的逻辑完全留在后端 Service，不重复写一遍。
 */

export const ACTION_CONFIG = {
  SUPPLEMENT_EVIDENCE: {
    api: 'supplementEvidence',
    endpoint: '/evidence',
    fields: [
      { key: 'evidenceUrl', label: '清晰的凭证地址（与说明至少填一项）', placeholder: '/mock/evidence/clear-photo.jpg' },
      { key: 'description', label: '补充说明', type: 'textarea' }
    ]
  },
  REQUEST_EVIDENCE: {
    api: 'review',
    endpoint: '/review',
    fields: [{ key: 'reason', label: '缺少什么材料（必填）', type: 'textarea', required: true }]
  },
  APPROVE_RETURN: {
    api: 'review',
    endpoint: '/review',
    fields: [{ key: 'reason', label: '退回指引（必填）：收件地址、包装与吊牌要求', type: 'textarea', required: true }]
  },
  REJECT_FROM_REVIEW: {
    api: 'review',
    endpoint: '/review',
    fields: [{ key: 'reason', label: '拒绝理由（必填，消费者要据此申诉）', type: 'textarea', required: true }]
  },
  REJECT_FROM_FINALIZE: {
    api: 'finalize',
    endpoint: '/finalize',
    fields: [{ key: 'note', label: '拒绝结论（必填）', type: 'textarea', required: true }]
  },
  RECEIVE: {
    api: 'receive',
    endpoint: '/receive',
    fields: [{ key: 'note', label: '收货备注：外包装与吊牌情况', placeholder: '包裹已签收，外包装完整' }]
  },
  INSPECT: {
    api: 'inspect',
    endpoint: '/inspect',
    fields: [
      {
        key: 'result',
        label: '验货结论',
        type: 'select',
        options: [
          { name: 'PASSED', label: '验货通过' },
          { name: 'ABNORMAL', label: '验货异常' }
        ]
      },
      { key: 'abnormalReason', label: '异常原因（选「验货异常」时必填）', type: 'textarea' }
    ]
  },
  CONFIRM_EXCHANGE: {
    api: 'finalize',
    endpoint: '/finalize',
    fields: [
      { key: 'reissueNo', label: '模拟补发单号（必填）', placeholder: 'SF0000000000000', required: true },
      { key: 'note', label: '结论说明', type: 'textarea' }
    ]
  },
  CONFIRM_REFUND: {
    api: 'finalize',
    endpoint: '/finalize',
    fields: [
      { key: 'refundAmount', label: '模拟退款金额（必填，单位元）', type: 'number', step: '0.01', required: true },
      { key: 'note', label: '结论说明', type: 'textarea' }
    ]
  }
};

/**
 * 「拒绝申请」在两个阶段都能发生：审核阶段走 /review，验货之后的终定阶段走 /finalize。
 * 后端 ReviewDecisionDTO 与 FinalDecisionDTO 的 action 都接受 REJECT，这里按当前状态选择接口。
 */
export function resolveActionConfig(actionName, view) {
  if (actionName !== 'REJECT') {
    return ACTION_CONFIG[actionName];
  }
  return view.status === 'PENDING_REVIEW' ? ACTION_CONFIG.REJECT_FROM_REVIEW : ACTION_CONFIG.REJECT_FROM_FINALIZE;
}
