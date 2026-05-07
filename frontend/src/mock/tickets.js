/**
 * 工单管理 Mock 数据
 * 状态：pending|processing|done|evaluated|closed
 * 紧急度：normal|urgent|critical
 * 故障类型：水电|网络|家具|其他
 */

export const MOCK_STAFF = [
  { userId: 2, realName: '李师傅', department: '后勤保障部', phone: '13800138001' },
  { userId: 5, realName: '王师傅', department: '后勤保障部', phone: '13800138004' },
  { userId: 6, realName: '赵师傅', department: '后勤保障部', phone: '13800138005' },
  { userId: 7, realName: '刘师傅', department: '网络中心', phone: '13800138006' },
  { userId: 8, realName: '张维修', department: '水电维修组', phone: '13800138007' },
  { userId: 9, realName: '周维修', department: '水电维修组', phone: '13800138008' },
  { userId: 10, realName: '孙维修', department: '土建维护组', phone: '13800138009' },
  { userId: 11, realName: '马维修', department: '土建维护组', phone: '13800138010' }
]

function makeTimeline(status, createTime) {
  const base = [
    { operator: '学生', time: createTime, remark: '提交报修' }
  ]
  if (['processing', 'done', 'evaluated', 'closed'].includes(status)) {
    base.push({ operator: '管理员', time: addMinutes(createTime, 5), remark: '工单已受理' })
    base.push({ operator: '管理员', time: addMinutes(createTime, 8), remark: '已指派李师傅处理' })
    base.push({ operator: '李师傅', time: addMinutes(createTime, 15), remark: '已到达现场，开始维修' })
  }
  if (['done', 'evaluated', 'closed'].includes(status)) {
    base.push({ operator: '李师傅', time: addMinutes(createTime, 45), remark: '维修完成，问题已解决' })
  }
  if (['evaluated'].includes(status)) {
    base.push({ operator: '学生', time: addMinutes(createTime, 60), remark: '已评价：服务满意' })
  }
  if (['closed'].includes(status)) {
    base.push({ operator: '管理员', time: addMinutes(createTime, 50), remark: '工单已关闭：重复提交' })
  }
  return base
}

function addMinutes(timeStr, mins) {
  const d = new Date(timeStr.replace(/-/g, '/'))
  d.setMinutes(d.getMinutes() + mins)
  return d.toISOString().slice(0, 16).replace('T', ' ')
}

export const MOCK_TICKETS = [
  {
    id: 1, ticketNo: 'T20260310001', title: '宿舍空调漏水严重',
    description: '宿舍空调开启后持续漏水，地面湿滑，存在安全隐患，请尽快处理。',
    reporterName: '张三', reporterNo: '2021001001', location: '宿舍B栋501',
    category: '水电', urgency: 'urgent', status: 'pending',
    images: [], createTime: '2026-03-10 09:00:00', phone: '13800138002',
    timeline: makeTimeline('pending', '2026-03-10 09:00:00')
  },
  {
    id: 2, ticketNo: 'T20260310002', title: '教室投影仪无法开机',
    description: '教学楼A302教室投影仪无法开机，指示灯不亮，影响正常教学。',
    reporterName: '李四', reporterNo: '2021002002', location: '教学楼A栋302',
    category: '其他', urgency: 'normal', status: 'processing',
    images: [], createTime: '2026-03-10 10:15:00', phone: '13800138003',
    assignedStaff: '李师傅', timeline: makeTimeline('processing', '2026-03-10 10:15:00')
  },
  {
    id: 3, ticketNo: 'T20260310003', title: '寝室网络频繁掉线',
    description: '宿舍WiFi信号不稳定，经常断线，影响在线学习。',
    reporterName: '王五', reporterNo: '2021003003', location: '宿舍C栋203',
    category: '网络', urgency: 'urgent', status: 'done',
    images: [], createTime: '2026-03-09 14:20:00', phone: '13800138007',
    assignedStaff: '王师傅', timeline: makeTimeline('done', '2026-03-09 14:20:00')
  },
  {
    id: 4, ticketNo: 'T20260310004', title: '桌椅损坏无法使用',
    description: '图书馆自习区桌椅腿断裂，无法正常使用。',
    reporterName: '赵六', reporterNo: '2021004004', location: '图书馆3楼',
    category: '家具', urgency: 'normal', status: 'evaluated',
    images: [], createTime: '2026-03-09 08:30:00', phone: '13800138008',
    assignedStaff: '赵师傅', timeline: makeTimeline('evaluated', '2026-03-09 08:30:00')
  },
  {
    id: 5, ticketNo: 'T20260310005', title: '水龙头漏水',
    description: '卫生间水龙头持续滴水，浪费水资源。',
    reporterName: '孙七', reporterNo: '2021005005', location: '宿舍A栋401',
    category: '水电', urgency: 'normal', status: 'closed',
    images: [], createTime: '2026-03-08 16:45:00', phone: '13800138009',
    timeline: makeTimeline('closed', '2026-03-08 16:45:00')
  },
  {
    id: 6, ticketNo: 'T20260310006', title: '特急：实验室断电',
    description: '实验室突然断电，实验设备受影响，需紧急处理！',
    reporterName: '周八', reporterNo: '2021006006', location: '实验楼2层',
    category: '水电', urgency: 'critical', status: 'pending',
    images: [], createTime: '2026-03-10 11:00:00', phone: '13800138010',
    timeline: makeTimeline('pending', '2026-03-10 11:00:00')
  },
  {
    id: 7, ticketNo: 'T20260310007', title: '宿舍门锁损坏',
    description: '宿舍门锁无法正常锁闭，存在安全隐患。',
    reporterName: '吴九', reporterNo: '2021007007', location: '宿舍D栋602',
    category: '家具', urgency: 'urgent', status: 'processing',
    images: [], createTime: '2026-03-10 08:00:00', phone: '13800138011',
    assignedStaff: '刘师傅', timeline: makeTimeline('processing', '2026-03-10 08:00:00')
  },
  {
    id: 8, ticketNo: 'T20260310008', title: '教室插座无电',
    description: '教学楼B205靠窗一排插座无电，无法使用电脑。',
    reporterName: '郑十', reporterNo: '2021008008', location: '教学楼B栋205',
    category: '水电', urgency: 'normal', status: 'pending',
    images: [], createTime: '2026-03-10 13:30:00', phone: '13800138012',
    timeline: makeTimeline('pending', '2026-03-10 13:30:00')
  },
  {
    id: 9, ticketNo: 'T20260310009', title: '校园网无法连接',
    description: '宿舍区部分楼栋有线网络无法连接，已重启路由器无效。',
    reporterName: '冯一', reporterNo: '2021009009', location: '宿舍E栋101',
    category: '网络', urgency: 'urgent', status: 'done',
    images: [], createTime: '2026-03-09 09:00:00', phone: '13800138013',
    assignedStaff: '王师傅', timeline: makeTimeline('done', '2026-03-09 09:00:00')
  },
  {
    id: 10, ticketNo: 'T20260310010', title: '寝室衣柜门脱落',
    description: '木质衣柜门铰链损坏，门板脱落。',
    reporterName: '陈二', reporterNo: '2021010010', location: '宿舍F栋303',
    category: '家具', urgency: 'normal', status: 'processing',
    images: [], createTime: '2026-03-10 10:00:00', phone: '13800138014',
    assignedStaff: '赵师傅', timeline: makeTimeline('processing', '2026-03-10 10:00:00')
  },
  {
    id: 11, ticketNo: 'T20260309001', title: '浴室热水器不热',
    description: '宿舍浴室热水器出水不热，无法洗热水澡。',
    reporterName: '楚三', reporterNo: '2021011011', location: '宿舍B栋302',
    category: '水电', urgency: 'urgent', status: 'evaluated',
    images: [], createTime: '2026-03-09 07:30:00', phone: '13800138015',
    assignedStaff: '李师傅', timeline: makeTimeline('evaluated', '2026-03-09 07:30:00')
  },
  {
    id: 12, ticketNo: 'T20260309002', title: '办公室打印机故障',
    description: '行政楼301办公室打印机卡纸且无法正常打印。',
    reporterName: '卫四', reporterNo: '2021012012', location: '行政楼301',
    category: '其他', urgency: 'normal', status: 'closed',
    images: [], createTime: '2026-03-09 11:20:00', phone: '13800138016',
    timeline: makeTimeline('closed', '2026-03-09 11:20:00')
  },
  {
    id: 13, ticketNo: 'T20260309003', title: '宿舍阳台灯不亮',
    description: '阳台照明灯不亮，夜晚无法使用阳台。',
    reporterName: '蒋五', reporterNo: '2021013013', location: '宿舍A栋205',
    category: '水电', urgency: 'normal', status: 'pending',
    images: [], createTime: '2026-03-09 15:00:00', phone: '13800138017',
    timeline: makeTimeline('pending', '2026-03-09 15:00:00')
  },
  {
    id: 14, ticketNo: 'T20260309004', title: '教学楼电梯故障',
    description: '教学楼C栋东侧电梯停在一楼无法运行，显示故障码。',
    reporterName: '沈六', reporterNo: '2021014014', location: '教学楼C栋',
    category: '其他', urgency: 'critical', status: 'processing',
    images: [], createTime: '2026-03-09 14:00:00', phone: '13800138018',
    assignedStaff: '刘师傅', timeline: makeTimeline('processing', '2026-03-09 14:00:00')
  },
  {
    id: 15, ticketNo: 'T20260309005', title: '宿舍床板松动',
    description: '上床下桌结构，上铺床板螺丝松动，睡觉时晃动。',
    reporterName: '韩七', reporterNo: '2021015015', location: '宿舍D栋405',
    category: '家具', urgency: 'urgent', status: 'done',
    images: [], createTime: '2026-03-08 10:30:00', phone: '13800138019',
    assignedStaff: '赵师傅', timeline: makeTimeline('done', '2026-03-08 10:30:00')
  },
  {
    id: 16, ticketNo: 'T20260308001', title: '机房电脑无法上网',
    description: '计算机机房部分机器无法连接校园网。',
    reporterName: '杨八', reporterNo: '2021016016', location: '信息楼机房',
    category: '网络', urgency: 'normal', status: 'evaluated',
    images: [], createTime: '2026-03-08 09:00:00', phone: '13800138020',
    assignedStaff: '王师傅', timeline: makeTimeline('evaluated', '2026-03-08 09:00:00')
  },
  {
    id: 17, ticketNo: 'T20260308002', title: '厕所冲水阀损坏',
    description: '公共厕所冲水阀无法按压，无法冲水。',
    reporterName: '朱九', reporterNo: '2021017017', location: '宿舍C栋1楼',
    category: '水电', urgency: 'urgent', status: 'closed',
    images: [], createTime: '2026-03-08 16:00:00', phone: '13800138021',
    timeline: makeTimeline('closed', '2026-03-08 16:00:00')
  },
  {
    id: 18, ticketNo: 'T20260308003', title: '自习室椅子损坏',
    description: '图书馆自习区一把椅子腿断裂。',
    reporterName: '秦十', reporterNo: '2021018018', location: '图书馆2楼',
    category: '家具', urgency: 'normal', status: 'pending',
    images: [], createTime: '2026-03-08 14:15:00', phone: '13800138022',
    timeline: makeTimeline('pending', '2026-03-08 14:15:00')
  },
  {
    id: 19, ticketNo: 'T20260307001', title: '宿舍空调不制冷',
    description: '空调开启制冷模式但吹出热风，怀疑缺氟。',
    reporterName: '尤一', reporterNo: '2021019019', location: '宿舍E栋505',
    category: '水电', urgency: 'urgent', status: 'done',
    images: [], createTime: '2026-03-07 11:00:00', phone: '13800138023',
    assignedStaff: '李师傅', timeline: makeTimeline('done', '2026-03-07 11:00:00')
  },
  {
    id: 20, ticketNo: 'T20260307002', title: '实验室网线不通',
    description: '实验台网线接口无信号，已更换网线仍无效。',
    reporterName: '许二', reporterNo: '2021020020', location: '实验楼3层',
    category: '网络', urgency: 'normal', status: 'processing',
    images: [], createTime: '2026-03-07 15:30:00', phone: '13800138024',
    assignedStaff: '王师傅', timeline: makeTimeline('processing', '2026-03-07 15:30:00')
  },
  {
    id: 21, ticketNo: 'T20260307003', title: '宿舍窗户关不严',
    description: '窗户合页损坏，无法完全关闭，有漏风。',
    reporterName: '何三', reporterNo: '2021021021', location: '宿舍F栋602',
    category: '家具', urgency: 'normal', status: 'evaluated',
    images: [], createTime: '2026-03-07 08:45:00', phone: '13800138025',
    assignedStaff: '赵师傅', timeline: makeTimeline('evaluated', '2026-03-07 08:45:00')
  },
  {
    id: 22, ticketNo: 'T20260306001', title: '教学楼漏水',
    description: '雨天教学楼4楼走廊天花板渗水，地面有积水。',
    reporterName: '吕四', reporterNo: '2021022022', location: '教学楼D栋4楼',
    category: '水电', urgency: 'critical', status: 'pending',
    images: [], createTime: '2026-03-06 10:00:00', phone: '13800138026',
    timeline: makeTimeline('pending', '2026-03-06 10:00:00')
  }
]
